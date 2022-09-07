# Developer Approach

## 1. Architecture
This diagram shows the basic architecture on which this application is based. I took into account that due to the high load of users and cars, this API Rest should be stateless to be able to deploy some instances and keep high scalability. This API could have sudden traffic spikes, so a better solution to the traditional stateful servers (in which this problem would be fixed by adding more power) is to deploy some instances of the API to share all the load between some servers.

![Architecture diagram](/images/diagram.png "Architecture diagram")

 
To develop this REST API, I have used Java 18 with Spring Boot because Java is robust, easy to use, widely adopted in the software industry, there is a lot of documentation, and it is platform-independent. Finally, it is the language in which I have been developing for the last few years.

To store all data, I have used an (embedded) Redis database and Lettuce as the connector because it is thread-safe. Redis is an open source (BSD licensed), in-memory data structure store used as a database, cache, message broker, and streaming engine. Redis provides data structures such as strings, hashes, lists... Although there are more NoSQL databases, Redis is faster because it is an in-memory database, so for those reasons, I think that Redis is perfect to achieve high performance for this challenge.
 
## 2. Redis data types
Redis has different data types, but I will only focus on the ones that I have implemented in this API Rest.

- Hashes: they are record types structured as collections of field-value pairs. They can be used to represent basic objects and to store groupings of counters, among other things. Most Redis hash commands are O(1). A few commands - such as HKEYS, HVALS, and HGETALL - are O(n), where n is the number of field-value pairs.

- Lists: they are Linked lists and can be used to implement stacks and queues. List operations that access its head or tail are O(1). However, commands that manipulate elements within a list are usually O(n). Examples of these include LINDEX, LINSERT, and LSET.

The reason to choose the Hashes is that they are only O(1) in reading and writing operations. However, a disadvantage is that data is saved without any order, hashes are collections of field-value pairs. So, for this basic approach, I assume that cars should not follow any order, so all the cars are saved in hashes depending on the seats available. Furthermore, all journeys are saved using this data type, because it is more efficient to have a hash with all the journeys to be very fast in reading and updating.

However, the problem arrives when the groups of people cannot be served instantly as they are registered. In this case, the arrival order must be kept and a Redis List is created to save only their IDs in a "waiting list". This approach is very efficient because commands to manipulate the list cost O(n), so it is better to reduce as much as possible the use of linked lists.

## 3. Algorithms
First, if a group of people arrives and there is a car available, then that car is assigned to the group. But if there is no car available, I have implemented a "priority algorithm" that only assigns a car if some drop-offs have been reached depending on the size of the car fleet.

This approach, although it is very simple, allows giving more priority to the first waiting group because maybe there is a group of 5 people waiting, and it is harder to assign a car for them if the cars are filled with groups of 1 or 2 people than assign a car for only 1 person.

For example, if there is a fleet of 10 cars, that size is multiplied by a constant of 0.3, so the drop-off limit is 3. If there is a group of 5 people waiting for a car, that group has the priority for a maximum of 3 tries, if after this, a car is not yet available, then the "blocking state" is broken and now all the waiting list is checked to find a group that could be served. Furthermore, if while the waiting list is being checked, the next following groups cannot be served, they are also penalized by adding a "waiting weight".

## 4. Tests
Unit tests using JUint 5 have been implemented to check that the API REST methods are correct and that the "priority algorithm" works as expected.

## 5. Documentation
All methods are commented and classes are documented using Javadoc. The generated result "index.html" is located in the "javadoc" directory.

## 6. Logging
Loggin is very important to keep a trace of what is happening in the program and to catch possible errors. Also, a "logs" directory is generated to save the traces. This has been implemented with Logback. 

## 7. Improvements
A great improvement would be to implement an "age queue" for the car fleet to give more priority to those cars that have not been assigned to a group for a longer period. In this way, car drivers would be happier because all of them will earn some money and the work would be better distributed.
