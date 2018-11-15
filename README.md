dialog-share-bot
==================

## Compilation

In order to run this bot you should get [java-bot-sdk](https://github.com/nabetse00/java-bot-sdk/) from my repo until
changes are merge to master.
Then run: `gradlew build` on linux or `gradlew.bat build` on windows.


## Launch

To run the bot: `gradlew run` on linux or `gradlew.bat run` on windows.
Don't forget to fill your bot token on Main.java.

## Deployment

[dialog.conf](dialog.conf) contains endpoint and host info:

```
dialog.botsdk {
  host = grpc-test.transmit.im
  port = 8080
  fork-join-pool.parallelism = 16
}
```


## Built With

* [Visual Studio Code](https://code.visualstudio.com/) - IDE
* [Gradle](https://gradle.org/) - Dependency Management
* [slf4j](https://www.slf4j.org/) - Simple Logging Facade for Java

## Authors

* [**Nabetse**](https://iteasys.com) 

See also the list of [contributors](https://github.com/nabetse00/java-bot-sdk/graphs/contributors) who participated in this project.

## License

This project is licensed under Apache V2 - see the [LICENSE.md](LICENSE.md) file for details

