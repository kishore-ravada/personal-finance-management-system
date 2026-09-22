FROM jenkins/jenkins:lts

USER root

# Install Maven, Node.js, npm and Docker CLI
RUN apt-get update && \
    apt-get install -y \
        maven \
        nodejs \
        npm \
        docker.io && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

USER jenkins