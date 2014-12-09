


```bash
cd $CCNL_HOME
docker build -t basilkohler/ccn-lite:devel .
docker run -d -p 9000:9000/udp --name ccnl basilkohler/ccn-lite:devel /var/ccn-lite/bin/ccn-nfn-relay -s ndn2013 -v 99 -u 9000
docker stop ccnl && docker rm ccnl
```

```bash
cd nfn-scala
docker build -t basilkohler/nfn-scala:devel .
docker run -d --name nfnscala --link ccnl:ccnl basilkohler/nfn-scala:devel
docker stop nfnscala && docker rm nfnscala
```
