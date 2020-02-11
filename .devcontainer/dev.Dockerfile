FROM maven:3-jdk-11-openj9

ADD chromedriver_linux /tmp/chromedriver

# install powerlevel10k for better command line experience
ENV TERM=${TERM}
ENV COLORTERM=${COLORTERM}
RUN apt update \
  && apt install -y git zsh \
  && git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ~/powerlevel10k \
  && echo "source ~/powerlevel10k/powerlevel10k.zsh-theme" >>~/.zshrc \
  && cd ~/powerlevel10k \
  && exec zsh
# you have to install fonts on your laptop (where your IDE editor/machine is running on) instead of inside the container
