package main


import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}


object TotalCount {
  def main(args: Array[String]) {
    if (args.length < 4) {
      System.out.println("usage: <zk--ip:port> <group> <topic> <numThreads>") 
      System.exit(1)
    }//四个输入参数
    Logger.getRootLogger.setLevel(Level.WARN)//屏蔽info级别的警告


    val totaltimes=(values: Seq[Int],state: Option[Int])=>{
      val currentCount=values.foldLeft(0)(_ + _)
      val previousCount=state.getOrElse(0)
      Some(currentCount+previousCount)
    }


    val Array(zkQuorum, group, topics, numThreads) = args
    val conf = new SparkConf()
    conf.setMaster("spark://sunxiong-3:7077").setAppName("TotalSort").set("spark.executor.memory", "256m")
    val ssc = new StreamingContext(conf, Seconds(2)) //以2秒为间隔
    ssc.checkpoint("checkpoint")
    val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap
   
    println(topicMap)
    

    val lines = KafkaUtils.createStream(ssc,zkQuorum,group,topicMap).map(_._2)

    val keyRegex = """\[[^\]]+]"""
    val keyWords = lines.map(line => keyRegex.r.findAllIn(line).mkString).filter(_!="")
    val wordCount = keyWords.map(word=>(word,1)).updateStateByKey[Int](totaltimes)
    val sorted = stawd.map(word=>(word._2,word._1)).transform(_.sortByKey(false)).map(word=>(word._2,word._1))

    sorted.print()
   
    ssc.start()
    ssc.awaitTermination()
  }
}
