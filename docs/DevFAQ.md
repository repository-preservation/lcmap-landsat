# Dev/Test Frequently Asked Questions

#### Test failure: "Caused by: com.datastax.driver.core.exceptions.InvalidQueryException: Undefined column name XXXXX"
The Cassandra schema is out of sync from what the code expects.  If running
Cassandra locally for development & test, the simplest way to solve this
is to remove all docker containers with `make docker-rm-all`.  Alternatively
remove all images with `make docker-rmi-all` followed by `make docker-test-up`
(or `make docker-dev-up`).  
Running `lein test` will automatically create the proper Cassandra schema before executing tests.

---
