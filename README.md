# detekt-sidekt

detekt extension that shamelessly kangs IntelliJ annotations. may probably not be needed once Jetbrains releases their
gradle/maven plugin to do the same.

Inspections provided:

 - **BlockingCallContext**: inspect blocking method calls within non-blocking context

## detekt run

requires type resolution, so ensure you are setting the `--classpath` argument

```bash
java -jar detekt-cli-1.16.0-all.jar \
  --plugins sidekt-1.0-SNAPSHOT.jar \
  --classpath 'dep1.jar:dep2.jar' \
  --jvm-target 1.8 \
  --input 'src/' \
  --config defalt-detekt-config.yml
```

## BlockingCallContext

```yml
build:
  maxIssues: 99999 # ...? not sure what's a better way to _force_ an error
  excludeCorrectable: false
  weights:
    BlockingCallContext: 100000

sidekt:
  BlockingCallContext:
    active: true
    debug: 'stderr' # or dir-path
    blockingMethodAnnotations: ['com.custom.annotations.BlockingCall']
    blockingMethodFqNames: ['com.custom.wrapper.runBlocking']
    blockingExceptionTypes: ['com.amazonaws.http.timers.client.SdkInterruptedException'] # only works with your own source, given @Throws is a SOURCE annotation
    ioDispatcherFqNames: ['com.custom.Dispatchers.DB'] # Dispatchers.IO added by default
    reclaimableMethodAnnotations: ['com.custom.annotations.ReclaimableBlockingCall'] # empty by default
    reclaimableMethodFqNames: ['com.custom.annotations.ReclaimableBlockingCall'] # some reclaimable java methods added by default

```

### blockingMethodAnnotations

these are the annotations which you may use in your code to denote blocking methods

### blockingMethodFqNames

in case the annotations marking the method as blocking cannot be inferred (probably due to annotation retention at SOURCE), then,
such known methods' FQ names can be configured

### blockingExceptionTypes

methods annotated with `@kotlin.jvm.Throws`, and which end up throwing certain exceptions, can be used to identify blocking
methods.

please note: this will only work for methods in your own source code, and cannot come from the classpath since `@kotlin.jvm.Throws`
annotation retention is at SOURCE

### ioDispatcherFqNames

blocking calls are allowed within specific dispatchers, namely, Dispatchers.IO; given they are specifically for the
purpose. in case you have an additional IO dispatcher, you may configure the same

### reclaimableMethodAnnotations

albeit dispatchers specifically meant for IO are meant for such "blocking" calls, there may be certain methods which
already provide non-blocking method alternatives. such methods may be annotated with a distinct annotation, and configured

### reclaimableMethodFqNames

same as `reclaimableMethodAnnotations`, but allowing you to provide FQ names to the methods
