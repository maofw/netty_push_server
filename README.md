netty_pusher
============

netty pusher framework，Currently It cantans server(SQL script) 、Java SDK and Android SDK ,iOS SDK is developing!

Based netty-4.0.23.Final + spring build the push server platform！Already through simple test，and it is worked good。You can used Java SDK in your owner server,and it get from the sdk fold.Also you can used Android SDK for your mobile development,it used very easy and simple！

Before you start ,must choose an oracle database for your server,and import the SQL script. Other database is developing!You can develop it yourself with your own database,only create a Java File in server project，in the package：com.xwtec.monitorpush.dao，and used another spring annotate for @Service("your named")。

