namespace java uk.ac.york.thrift.es.api

struct Filter {
  1: string pattern
}

struct Notification {
  1: string message
}

service Publisher {
  # Implicitly subcribes the caller to a stream of notifications.
  oneway void subscribe(1: Filter p)
}

service Subscriber {
  # Receives a single notification from the publisher.
  oneway void notify(1: Notification n)
}
