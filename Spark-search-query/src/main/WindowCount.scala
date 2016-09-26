package main


import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}


object WindowCount{
  def main(args: Array[String]) {
    if (args.length < 4) {
      System.out.println("usage: Kafkalog <zk--ip:port> <group> <topics> <numThreads>")
      System.exit(1)
    }//四个输入参数
    Logger.getRootLogger.setLevel(Level.WARN)//屏蔽info级别的log
 
   val Array(zkQuorum, group, topics, numThreads) = args
    val conf = new SparkConf()
    conf.setMaster("spark://sunxiong-3:7077").setAppName("WindowCount").set("spark.executor.memory", "256m")
    val ssc = new StreamingContext(conf, Seconds(2)) //创建sparkContext 将数据流切分为2秒间隔
    ssc.checkpoint("checkpoint")//容错和恢复
    val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap //展成map
    println(topicMap)
    
    val lines = KafkaUtils.createStream(ssc,zkQuorum,group,topicMap).map(_._2) //创建与kafka数据流连接，返回Dstream
    
    val keyRegex = """\[[^\]]+]""" //匹配搜索词
    val keyWords = lines.map(line => keyRegex.r.findAllIn(line).mkString).filter(_!="")
    val wordCount = keyWords.map(word=>(word,1)).reduceByKeyAndWindow(_+_,_-_,Minutes(5),Seconds(2))//窗口长度五分钟
    val sorted = wordCount.map(word=>(word._2,word._1)).transform(_.sortByKey(false)).map(word=>(word._2,word._1))//交换两次

    sorted.print()
    ssc.start() // start program
    ssc.awaitTermination() //等待程序结束
  }
}
