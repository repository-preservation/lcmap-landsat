# Dev/Test Frequently Asked Questions

### Test failure: "Caused by: com.datastax.driver.core.exceptions.InvalidQueryException: Undefined column name XXXXX"
The Cassandra schema is out of sync from what the code expects.  If running
Cassandra locally for development & test, the simplest way to solve this
is to stop all docker processes with `docker-compose down` and then remove
all docker containers & images with `bin/docker-super-clean` followed by `docker-compose up`.  Running
`lein test` will automatically create the proper Cassandra schema before
executing tests.
