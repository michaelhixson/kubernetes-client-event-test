package rnd;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Main {
  private Main() {}

  public static void main(String[] args) throws Exception {
    var applyWorkaround = args.length > 0 && args[0].equals("workaround");

    if (applyWorkaround) {
      System.out.println("Applying the workaround");

      KubernetesDeserializer.registerProvider(
          () -> Map.of("v1#Event", Event.class));

    } else {
      System.out.println(
          "Not applying the workaround, letting the problem occur");
    }

    try (var client = new DefaultKubernetesClient()) {

      var namespaceName =
          "kubernetes-client-event-test-" + System.currentTimeMillis();

      var namespace =
          new NamespaceBuilder()
              .withNewMetadata()
              .withName(namespaceName)
              .endMetadata()
              .build();

      client.namespaces().create(namespace);

      System.out.println("Created namespace " + namespaceName);

      try {

        var countdown = new CountDownLatch(1);

        var watcher =
            new Watcher<Event>() {
              @Override
              public void eventReceived(Action action, Event resource) {
                System.out.println(
                    "eventReceived action=" + action
                        + ", resource=" + resource);

                countdown.countDown();
              }

              @Override
              public void onClose(KubernetesClientException cause) {
                System.out.println("onClose cause=" + cause);
              }
            };

        try (var watch = client.v1().events().inNamespace(namespaceName).watch(watcher)) {

          var pod =
              new PodBuilder()
                  .withNewMetadata()
                  .withName("test-pod")
                  .withNamespace(namespaceName)
                  .addToLabels("app", "test-app")
                  .endMetadata()
                  .withNewSpec()
                  .addNewContainer()
                  .withName("test-container")
                  .withImage("nginx:latest")
                  .endContainer()
                  .endSpec()
                  .build();

          client.pods().inNamespace(namespaceName).createOrReplace(pod);

          System.out.println("Added the Pod.  Waiting...");

          // We *should* receive an event for the pod being added...
          var eventWasReceived = countdown.await(5, TimeUnit.SECONDS);

          if (eventWasReceived) {
            System.out.println("We received the event!  The problem is fixed!");

          } else {
            System.out.println(
                "No event received, meaning the problem occurred.  "
                    + "Look for JSON deserialization stack traces in this "
                    + "program's output.");
          }
        }

      } finally {
        client.namespaces().delete(namespace);
      }
    }
  }
}
