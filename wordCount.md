# 網頁字頻計算
目標：使用hadoop MapReduce計算網頁字頻，網頁數量達某種程度。
方法：先將要計算的網頁搜集到hdfs儲存，再使用FileInputFormat.addInputPath()設定輸入資料夾。交給預設去分配工作。
To do：1. 不下載網頁內容，直接計算各網頁字頻。
2. 改token parsing，如：不計算標點符號

## 結果
編譯
```shell=
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main WordCount.java
jar cf wc.jar WordCount*.class
```
執行
```shell=
bin/hadoop jar wc.jar WordCount https://hadoop.apache.org /user/hadoop/wordcount/output
```
結果
```shell=
bin/hadoop fs -cat wordcount/output
```

## 程式碼
### 抓網頁
使用jsoup抓取某網站的所有連結，再抓取連結網頁的內容。抓取後暫存入input directory(暫定抓上限20個網頁)。
#### 使用jsoup
1. Download jsoup-x.x.x.jar
2. Add `/usr/local/hadoop/jsoupp-x.x.x.jar` to `HADOOP_CLASSPATH`

### 計算字頻
（取自hadoop document example code）
定義mapper、定義combiner、定義reducer，此處mapper和combiner使用同一個方法。
combiner用意在減少intermidiate key-value pair。
定義output型別
設定input資料夾
設定output（計算結果）資料夾

## Reference
[MapReduce Tutorial](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html)

