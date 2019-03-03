# running

Basic CRUD app with a REST API for running data, with some other things bolted on. API lives at `/api/v1/` and has a swagger UI at `/swagger-ui`. 

Some of the things bolted on are things I ask myself, like "how far have I run this calendar week/month/year?" or "How far have I run in the past week/month/quarter/year?".


There is a front-end to the service as well, but it is a work in progress.

generated using Luminus version "2.9.12.62"


## Prerequisites

You will need [Leiningen][1] 2.0 or above installed. You'll also need a PosgreSQL database and to create a secret for signing tokens. Both can be specified in your config.edn or via command-line parameters.
[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run 

## License

