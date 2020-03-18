FROM maven:3-jdk-11-openj9

ADD chromedriver_linux /tmp/chromedriver

# install tools that are useful for development
ENV TERM=${TERM}
ENV COLORTERM=${COLORTERM}
RUN echo "Installing dependencies..." \
  && apt-get update -y \
  # install latest git 2.17, husky requires > X.13
  && apt-get install git -y \
  && git --version \
  # install zsh
  && apt-get install zsh -y \
  # install oh-my-zsh for useful cli alias: https://github.com/ohmyzsh/ohmyzsh
  && sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" \
  # install powerlevel10k
  && git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ~/powerlevel10k \
  && echo "source ~/powerlevel10k/powerlevel10k.zsh-theme" >>~/.zshrc \
  && cd ~/powerlevel10k \
  && exec zsh
  # you have to install fonts on your laptop (where your IDE editor/machine is running on) instead of inside the container

RUN echo "Installing nodejs..." \
  && sh -c "$(curl -sL https://deb.nodesource.com/setup_12.x)" \
  && apt-get install -y nodejs

# https://github.com/microsoft/vscode-dev-containers/tree/master/containers/docker-in-docker
RUN echo "Installing docker CE CLI..." \ 
  && apt-get update \
  && apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common lsb-release \
  && curl -fsSL https://download.docker.com/linux/$(lsb_release -is | tr '[:upper:]' '[:lower:]')/gpg | apt-key add - 2>/dev/null \
  && add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/$(lsb_release -is | tr '[:upper:]' '[:lower:]') $(lsb_release -cs) stable" \
  && apt-get update \
  && apt-get install -y docker-ce-cli
