package tl.util

import kio.util.child
import kio.util.execute
import kio.util.nullStreamHandler
import tl.ppssppHome
import java.io.File

fun startPpssppWithIso(iso: File) {
  println("Starting PPSSPP...")
  execute(
    ppssppHome.child("PPSSPPWindows.exe"),
    args = listOf(iso),
    streamHandler = nullStreamHandler()
  )
}
