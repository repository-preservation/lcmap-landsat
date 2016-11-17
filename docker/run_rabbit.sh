#!/usr/bin/env bash
RABBIT_NAME="test-rabbit"
MANAGEMENT_PORT=8080
RABBIT_PORT=5672
echo "-------- Stopping $RABBIT_NAME -------- "
docker stop $RABBIT_NAME
echo "-------- Removing $RABBIT_NAME -------- "
docker rm $RABBIT_NAME
echo "-------- Starting $RABBIT_NAME -------- "
docker run -d --hostname my-rabbit --name $RABBIT_NAME -p $MANAGEMENT_PORT:15672 -p $RABBIT_PORT:5672 -p 5671:5671 rabbitmq:3-management
echo "-------- Rabbit Started on port $RABBIT_PORT, management app on port $MANAGEMENT_PORT -------- "
docker ps -a |grep $RABBIT_NAME
