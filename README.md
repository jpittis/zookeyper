A caching key value store on top of Zookeeper. (It's basically a write through cache.)

## Why

Zookeyper was a chance for me to write a "real world" Clojure project and experiment with
Zookeeper watches.

Though I don't recomend using this in production, you're welcome to use it for reference.
Keep in mind that:

- [zookeeper-clj](https://github.com/liebke/zookeeper-clj) hasn't had any activity for
  years. (It seems stable though!)
- I don't handle many edge cases that you'd normally want to handle in production.
- I don't know Clojure best practices. (This is the most complex thing I've written in
  Clojure!)


## Development

```bash
./zk/start.sh # Start a local Zookeeper.
lein test     # Run the test suite.
...           # Do some development.
./zk/stop.sh  # Stop the local Zookeeper.
```
