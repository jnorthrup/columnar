# Running the Java examples

First, go to the `src/java` directory and run `mvn install` to install the library to your local Maven repository.

After you have done this, you can come to this directory and run `mvn compile`.

Then, to run the server run:

    mvn exec:java -Dexec.mainClass=helloworld.GreeterServer -Dexec.args=9009

Observe the number that it prints in the last line. This is the port that the gossiper is listening on.

To start the client, run the following, substituting the port printed above:

    mvn exec:java -Dexec.mainClass=helloworld.GreeterClient -Dexec.args=127.0.0.1:${port}
