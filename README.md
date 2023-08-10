## Installation Steps

### 1. Clone Repo
```
  git clone git@github.com:Cogoport/ares.git
```
### 2. Install Docker and Docker Compose
[Docker and Docker compose Install](https://docs.docker.com/desktop/mac/install/)

### 3. Create ENV files

```
  cp ./api/.env.example ./api/.env
```
### 4. Run Docker compose
```
  docker-compose -f ./api/docker-compose.dev.yml up
```   

## Working with IntellJ IDE


We have to install jdk explicitly to use Intellj build and run functionality.
### Download GraalVM JDK 17
1. [JDK URL](https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.0.0.2/graalvm-ce-java17-darwin-amd64-22.0.0.2.tar.gz)



2. Go to Downloads folder and run following command
   ```
   tar -xzf graalvm-ce-java17-darwin-amd64-22.0.0.2.tar.gz
   ```
3. Move the downloaded package to its proper location using following command.
  ```
    sudo mv graalvm-ce-java17-22.0.0.2 /Library/Java/JavaVirtualMachines
  ```
4. [Update JDK path in Intellj](https://www.jetbrains.com/help/idea/sdk.html#change-project-sdk)


5. Then Run the application
### Voila!!
The application must have been started on port http://localhost:8080

