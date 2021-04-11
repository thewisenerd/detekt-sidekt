# detekt-sidekt

detekt extension to inspect blocking method calls within non-blocking context

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

## detekt-config

```yml
build:
  maxIssues: 99999 # ...? not sure what's a better way to _force_ an error
  excludeCorrectable: false
  weights:
    BlockingCallContext: 100000

sidekt:
  BlockingCallContext:
    active: true
    debug: 'stdout' # or dir-path
    blockingMethodAnnotations: ['com.custom.annotations.BlockingCall']

```
