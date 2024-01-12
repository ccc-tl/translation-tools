package tl.cli

import kio.util.child
import kio.util.execute
import kio.util.relativizePath
import kio.util.walkDir
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  if (args.size != 4) {
    println("Usage: [at3ToolBin] [oggenc2ToolBin] [inDir] [outDir]")
    exitProcess(1)
  }
  runBlocking {
    BatchAt3ToOgg(File(args[0]), File(args[1])).convert(File(args[2]), File(args[3]))
  }
  println("Done")
}

class BatchAt3ToOgg(private val at3Tool: File, private val oggenc2Tool: File, private val threads: Int = 4) {
  @OptIn(DelicateCoroutinesApi::class)
  suspend fun convert(inDir: File, outDir: File) {
    outDir.mkdir()
    val ctx = newFixedThreadPoolContext(threads, "ConvertSoundPool")
    val jobs = mutableListOf<Job>()
    walkDir(inDir) { inFile ->
      val job = GlobalScope.launch(ctx) {
        val relPath = inFile.relativizePath(inDir)
        println("Processing $relPath")
        val wavOut = outDir.child(relPath.replaceAfterLast(".", "wav"))
        val oggOut = outDir.child(relPath.replaceAfterLast(".", "ogg"))
        oggOut.parentFile.mkdirs()
        execute(at3Tool, args = listOf("-d", inFile, wavOut), workingDirectory = outDir)
        execute(oggenc2Tool, args = listOf("-Q", "-o", oggOut, wavOut), workingDirectory = outDir)
        wavOut.delete()
      }
      jobs.add(job)
    }
    jobs.forEach { it.join() }
    ctx.close()
  }
}
