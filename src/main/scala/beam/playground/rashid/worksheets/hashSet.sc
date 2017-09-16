import scala.collection.mutable

val hashSet1: mutable.HashSet[String] = mutable.HashSet.empty[String]

hashSet1 += "Vanilla Donut"

hashSet1 += "Vanilla Donut1"

hashSet1 += " 235234 asf"

hashSet1 map (x => println(x + ";"))

hashSet1.foreach(println)

println(hashSet1)




