# Basic Leader Worker System

### Link for Video: https://youtu.be/2cZcpulqsyg

## How to Run
The program will automatically default to port 9000 and localhost if you simply use gradle runLeader and gradle runWorker.
If you want to change the port or host IP, you can run:

- gradle runLeader --args "[port]"

- gradle runWorker --args "[host IP] [port]"

## Program Description
The system uses a JSON-based protocol design. Each protocol message includes a type field along with the additional information needed for that message, such as an integer value or a string message. The consensus algorithm works by keeping a results map where the key is the task ID and the value is a list of worker responses. When all active workers have responded, tracked by a counter that records how many workers are currently connected, the system detects that consensus is ready to be evaluated. A second map is created where the key is a response value and the value is the number of workers who voted for it. The list of worker responses is iterated through to populate this map. Then the map is iterated again to find the value with the highest vote count, while also checking for ties. Ties are handled by randomly selecting one of the tied values. Once a candidate result is chosen, the system checks whether the number of votes meets at least 50% of all active workers. If it does, the consensus result is printed on both the leader and worker sides. If not, the system prints a summary showing how all the votes were distributed. Worker failures are handled by the client threads within the server. If a worker crashes or disconnects, a finally block in the server’s client handler updates the worker list, ensuring the leader knows it lost a worker. The biggest limitation of the system is that it can only process one task at a time. Once a task is issued, the leader waits for all workers to respond before sending the next task. Additionally, tasks and responses must be integers as the system cannot handle floats. There is also no timeout mechanism, meaning the leader can get stuck if a worker remains connected but stops responding.
