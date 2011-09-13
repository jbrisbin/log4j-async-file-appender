# AsyncFileAppender

This is a Log4J appender that uses JDK 7's new AsynchronousFileChannel as the log writer.
Java 7's new async file handling is extremely fast and scalable and this extends that capability
to your logging system.

To use it, define the appender like you would any normal appender:

    <appender name="async" class="com.jbrisbin.async.logging.AsyncFileAppender">
      <param name="path" value="/path/to/file.log"/>
      <param name="threads" value="3"/>
      <param name="append" value="true"/>
      <layout class="org.apache.log4j.PatternLayout">
        <param name="ConversionPattern" value="%-5p: %c - %m%n"/>
      </layout>
    </appender>

The important parameter is the "path" parameter, which tells the appender the location of
the file you want to append to and whether you want the log file to be truncated every time you
open it or not (just set append=false to have the file truncated). The default is "true", meaning
the file will not be overwritten but appended to each time your process is started.

There is only one knob to tweak. You can set the number of threads you want to use for doing the
asynchronous file access. By default, it will use 3/4 of the processors available in your machine
(rounded up to 1 if you only have a single processor...do they even make those any more?). So for
my machine, the AsynchronousFileChannel instance gets 3 threads to work with when writing to the file.
This number isn't arbitrary and I've tested various thread pool sizes for AsynchronousFileChannel.
This seems to give excellent throughput and should be able to provide a noticeable performance
improvement to code that does a lot of logging.

It doesn't do any kind of rolling yet (size or date-based). That's something I'll likely add soon, though.

As with everything I do, it's Apache 2.0 licensed.