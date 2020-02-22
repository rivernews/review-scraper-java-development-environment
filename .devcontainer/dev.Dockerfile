FROM maven:3-jdk-11-openj9

ADD chromedriver_linux /tmp/chromedriver

# # install powerlevel10k for better command line experience
# ENV TERM=${TERM}
# ENV COLORTERM=${COLORTERM}
# RUN apt update \
#   && apt install -y git zsh \
#   && git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ~/powerlevel10k \
#   && echo "source ~/powerlevel10k/powerlevel10k.zsh-theme" >>~/.zshrc \
#   && cd ~/powerlevel10k \
#   && exec zsh
# # you have to install fonts on your laptop (where your IDE editor/machine is running on) instead of inside the container



# install tools that are useful for development
ENV TERM=${TERM}
ENV COLORTERM=${COLORTERM}
RUN echo "Installing dependencies..." \
  # install latest git 2.17, husky requires > X.13
  && apt-get update -y \
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
