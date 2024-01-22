FROM clojure:lein-bookworm as builder

COPY . /active-grafana
WORKDIR /active-grafana
RUN lein uberjar

FROM debian:bookworm as native

RUN apt update
RUN apt install -yy build-essential libz-dev zlib1g-dev wget

WORKDIR /opt

RUN wget https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz
RUN tar -xzf graalvm-jdk-21_linux-x64_bin.tar.gz
ENV JAVA_HOME=/opt/graalvm-jdk-21.0.2+13.1
ENV PATH=/opt/graalvm-jdk-21.0.2+13.1/bin:$PATH

WORKDIR /active-grafana

COPY --from=builder /active-grafana/target/active-grafana-0.1.0-SNAPSHOT-standalone.jar /active-grafana/active-grafana.jar

RUN native-image --no-fallback --features=clj_easy.graal_build_time.InitClojureClasses --enable-url-protocols=http --enable-url-protocols=https -jar active-grafana.jar active-grafana

FROM babashka/babashka:1.3.188

WORKDIR /opt/active-grafana
COPY --from=native /active-grafana/active-grafana .

