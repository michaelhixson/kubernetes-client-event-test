This is a demonstration of an issue with event watchers in
[kubernetes-client](https://github.com/fabric8io/kubernetes-client) 4.10.2.

To run while demonstrating the problem:

```
mvn clean compile exec:java
```

To run while demonstrating a workaround for the problem:

```
mvn clean compile exec:java -Dexec.args="workaround"
```

Original testing environment:

* Windows 10
* Java 14.0.1
* Maven 3.6.3
* Docker Desktop 2.3.0.3
* Docker: 19.03.8
* Kubernetes: v1.16.5
