A simple HTTP based write though key value cache built on top of Zookeeper.

## Why

This was a chance for me to write a "real world" Clojure project and experiment with
Zookeeper watches. Though I don't recomend using this in production, you're welcome to use
it for reference. Keep in mind that:

- [zookeeper-clj](https://github.com/liebke/zookeeper-clj) hasn't had any activity for
  years. (It seems stable though!)
- I don't handle many edge cases that you'd normally want to handle in production.
- I don't know Clojure best practices. (This is the most complex thing I've written in
  Clojure!)

# File Structure

- [core.clj](https://github.com/jpittis/zookeyper/blob/master/src/zookeyper/core.clj)
  parses command line args, connects to Zookeeper and starts an HTTP server.
- [app.clj](https://github.com/jpittis/zookeyper/blob/master/src/zookeyper/app.clj)
  defines HTTP handlers for interacting with the cache.
- [zookeeper.clj](https://github.com/jpittis/zookeyper/blob/master/src/zookeyper/zookeeper.clj)
  defines the state object of the cache and connection, key value interfaces
  with Zookeeper, and async cache updates using Zookeeper watches.
- [core_test.clj](https://github.com/jpittis/zookeyper/blob/master/test/zookeyper/core_test.clj)
  contains tests and test helpers.

## Usage

```
$ zookeyper --help
Usage: zookeyper <options>
options:
  -p, --port PORT       3333            Listen port number
  -z, --zk-hosts HOSTS  127.0.0.1:2181  Zookeeper comma separated hosts
  -h, --help
```

## Development

```bash
$ ./zk/start.sh # Start a local Zookeeper.
$ lein test     # Run the test suite.
...             # Do some development.
$ ./zk/stop.sh  # Stop the local Zookeeper.
```
