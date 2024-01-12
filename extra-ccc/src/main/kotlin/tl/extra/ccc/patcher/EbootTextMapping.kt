package tl.extra.ccc.patcher

import kio.util.highBits
import kio.util.lowBits
import kmips.Reg
import kmips.Reg.*
import kmipsx.elf.ElfPatch
import kmipsx.util.word
import tl.extra.ccc.patcher.translation.CccTranslation

fun provideEbootTextMappings(translation: CccTranslation): MutableList<EbootTextEntry> {
  fun patch(init: ElfPatch.() -> Unit): ElfPatch {
    val patch = ElfPatch(
      "EbootTranslationHelper", 0x08804000,
      enabled = true, autoRemoveRelocations = true, useRelativeChangeAddress = true
    )
    patch.init()
    return patch
  }

  fun textTablePatch(targetAddr: Int, textVirtualAddr: Int) = patch {
    change(targetAddr) { word(textVirtualAddr - 0x08804000) }
  }

  fun stdLoadAddress(targetRegister: Reg, targetAddrHiBits: Int, targetAddrLowBits: Int, textVirtualAddr: Int) =
    patch {
      change(targetAddrHiBits) { lui(targetRegister, textVirtualAddr.highBits()) }
      change(targetAddrLowBits) { ori(targetRegister, targetRegister, textVirtualAddr.lowBits()) }
    }

  fun stdLoadAddressImm(targetRegister: Reg, targetAddrHighBits: Int, textVirtualAddress: Int): ElfPatch {
    return stdLoadAddress(targetRegister, targetAddrHighBits, targetAddrHighBits + 4, textVirtualAddress)
  }

  val entries = mutableListOf<EbootTextEntry>()
  with(entries) {
    add(EbootTextEntry(0, 7, 0x1A3AE8))
    add(
      EbootTextEntry(
        1, 7, 0x1A3AF0,
        auxPatch = arrayOf({ stdLoadAddress(a1, 0x00003150, 0x0000315C, it) })
      )
    )
    add(EbootTextEntry(2, 7, 0x1A3AF8))
    add(EbootTextEntry(3, 11, 0x1ABDC8))
    add(EbootTextEntry(4, 11, 0x1ABDD4))
    add(EbootTextEntry(5, 11, 0x1ABDE0))
    add(EbootTextEntry(6, 15, 0x1ABDEC))
    add(EbootTextEntry(7, 11, 0x1ABDFC))
    add(EbootTextEntry(8, 11, 0x1ABE08))
    add(EbootTextEntry(9, 11, 0x1ABE14))
    add(EbootTextEntry(10, 7, 0x1ABE20))
    add(EbootTextEntry(11, 11, 0x1ABE28))
    add(EbootTextEntry(12, 7, 0x1ABE34))
    add(EbootTextEntry(13, 15, 0x1ABE3C))
    add(EbootTextEntry(14, 19, 0x1ABE4C))
    add(EbootTextEntry(15, 15, 0x1ABE60))
    add(EbootTextEntry(16, 19, 0x1ABE70))
    add(EbootTextEntry(17, 15, 0x1ABE84))
    add(EbootTextEntry(18, 15, 0x1ABE94))
    add(EbootTextEntry(19, 19, 0x1ABEA4))
    add(EbootTextEntry(20, 7, 0x1ABEB8))
    add(EbootTextEntry(21, 11, 0x1ABEC0))
    add(EbootTextEntry(22, 11, 0x1ABECC))
    add(EbootTextEntry(23, 11, 0x1ABED8))
    add(EbootTextEntry(24, 7, 0x1ABEE4))
    add(EbootTextEntry(25, 7, 0x1ABEEC))
    add(
      EbootTextEntry(
        26, 7, 0x1ABEF4,
        auxPatch = arrayOf({ textTablePatch(0x002243E8, it) })
      )
    )
    add(EbootTextEntry(27, 11, 0x1ABEFC))
    add(EbootTextEntry(28, 11, 0x1ABF08))
    add(EbootTextEntry(29, 11, 0x1ABF14))
    add(EbootTextEntry(30, 11, 0x1ABF20))
    add(EbootTextEntry(31, 15, 0x1ABF2C))
    add(EbootTextEntry(32, 11, 0x1ABF3C))
    add(EbootTextEntry(33, 15, 0x1ABF48))
    add(EbootTextEntry(34, 11, 0x1ABF58))
    add(EbootTextEntry(35, 15, 0x1ABF64))
    add(
      EbootTextEntry(
        36, 7, 0x1ABF74,
        auxPatch = arrayOf({ textTablePatch(0x00224410, it) })
      )
    )
    add(EbootTextEntry(37, 11, 0x1ABF7C))
    add(EbootTextEntry(38, 15, 0x1ABF88))
    add(EbootTextEntry(39, 11, 0x1ABF98))
    add(EbootTextEntry(40, 11, 0x1ABFA4))
    add(EbootTextEntry(41, 7, 0x1ABFB0))
    add(EbootTextEntry(42, 11, 0x1ABFB8))
    add(EbootTextEntry(43, 19, 0x1ABFC4))
    // 44-47 already handled by name entry patch
    add(EbootTextEntry(44, 0, -1, skip = true))
    add(EbootTextEntry(45, 0, -1, skip = true))
    add(EbootTextEntry(46, 0, -1, skip = true))
    add(EbootTextEntry(47, 0, -1, skip = true))
    add(EbootTextEntry(48, 7, 0x1B5BF4))
    add(EbootTextEntry(48, 7, 0x21B3D4))
    add(
      EbootTextEntry(
        49, 7, 0x1B1138,
        auxPatch = arrayOf({ textTablePatch(0x002248B8, it) })
      )
    )
    add(
      EbootTextEntry(
        49, 7, 0x1B78C0,
        auxPatch = arrayOf({ textTablePatch(0x002253D8, it) })
      )
    )
    add(EbootTextEntry(50, 15, 0x1B78D4, skip = true))
    add(EbootTextEntry(51, 23, 0x1B78E4))
    add(
      EbootTextEntry(
        52, 23, 0x1B790C,
        auxPatch = arrayOf({
          patch {
            change(0x000B8844) { lui(a1, it.highBits()) }
            change(0x000B88D4) { lui(a1, it.highBits()) }
            change(0x000B88E4) { ori(a1, a1, it.lowBits()) }

            // this is for "0 1 2 3 4 5 6 7 8 9" from above to copy only one ASCII character for formatted string
            change(0x000B882C) { li(a2, 1) }
            change(0x000B883C) { li(a2, 1) }
            change(0x000B88B0) { li(a2, 1) }
            change(0x000B88C0) { li(a2, 1) }
            change(0x000B88D0) { li(a2, 1) }
            // fix positions where to copy that single ascii character
            change(0x000B87D0) { addiu(s4, s2, 1) }
            change(0x000B88C4) { addiu(a0, s2, 2) }
          }
        })
      )
    )

    add(
      EbootTextEntry(
        53, 7, 0x217FA8,
        auxPatch = arrayOf(
          { textTablePatch(0x0021858C, it) },
          { textTablePatch(0x002185D4, it) },
          { textTablePatch(0x0021862C, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        54, 15, 0x217FB0,
        auxPatch = arrayOf(
          { textTablePatch(0x00218594, it) },
          { textTablePatch(0x002185DC, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        55, 15, 0x217FC0,
        auxPatch = arrayOf(
          { textTablePatch(0x0021859C, it) },
          { textTablePatch(0x002185E4, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        56, 15, 0x217FD0,
        auxPatch = arrayOf({ textTablePatch(0x002185A4, it) })
      )
    )
    add(EbootTextEntry(57, 11, 0x217FE0))
    add(
      EbootTextEntry(
        58, 11, 0x217FEC,
        auxPatch = arrayOf({ textTablePatch(0x002185B4, it) })
      )
    )
    add(
      EbootTextEntry(
        59, 7, 0x217FF8,
        auxPatch = arrayOf(
          { textTablePatch(0x002185BC, it) },
          { textTablePatch(0x002185FC, it) }
        )
      )
    )
    add(EbootTextEntry(60, 7, 0x218000))
    add(EbootTextEntry(61, 7, 0x218008))
    add(
      EbootTextEntry(
        62, 15, 0x218010,
        auxPatch = arrayOf({ textTablePatch(0x002185EC, it) })
      )
    )
    add(
      EbootTextEntry(
        63, 11, 0x218020,
        auxPatch = arrayOf({ textTablePatch(0x002185F4, it) })
      )
    )
    add(
      EbootTextEntry(
        64, 11, 0x21802C,
        auxPatch = arrayOf(
          { textTablePatch(0x00218604, it) },
          { textTablePatch(0x00218624, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        65, 7, 0x218038,
        auxPatch = arrayOf({ textTablePatch(0x0021860C, it) })
      )
    )
    add(EbootTextEntry(66, 11, 0x218040))
    add(EbootTextEntry(67, 7, 0x21804C))
    add(
      EbootTextEntry(
        68, 15, 0x218054,
        auxPatch = arrayOf({ textTablePatch(0x0021863C, it) })
      )
    )
    add(
      EbootTextEntry(
        69, 15, 0x218064,
        auxPatch = arrayOf({ textTablePatch(0x00218644, it) })
      )
    )
    add(
      EbootTextEntry(
        70, 15, 0x218074,
        auxPatch = arrayOf(
          { textTablePatch(0x0021864C, it) },
          { textTablePatch(0x002187EC, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        71, 15, 0x218084,
        auxPatch = arrayOf({ textTablePatch(0x00218654, it) })
      )
    )
    add(
      EbootTextEntry(
        72, 15, 0x218094,
        auxPatch = arrayOf({ textTablePatch(0x0021865C, it) })
      )
    )
    add(
      EbootTextEntry(
        73, 15, 0x2180A4,
        auxPatch = arrayOf(
          { textTablePatch(0x00218664, it) },
          { textTablePatch(0x002187F4, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        74, 15, 0x2180B4,
        auxPatch = arrayOf({ textTablePatch(0x0021866C, it) })
      )
    )
    add(
      EbootTextEntry(
        75, 15, 0x2180C4,
        auxPatch = arrayOf({ textTablePatch(0x00218674, it) })
      )
    )
    add(
      EbootTextEntry(
        76, 15, 0x2180D4,
        auxPatch = arrayOf(
          { textTablePatch(0x0021867C, it) },
          { textTablePatch(0x002187FC, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        77, 19, 0x2180E4,
        auxPatch = arrayOf({ textTablePatch(0x00218684, it) })
      )
    )
    add(
      EbootTextEntry(
        78, 19, 0x2180F8,
        auxPatch = arrayOf({ textTablePatch(0x0021868C, it) })
      )
    )
    add(
      EbootTextEntry(
        79, 19, 0x21810C,
        auxPatch = arrayOf(
          { textTablePatch(0x00218694, it) },
          { textTablePatch(0x00218804, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        80, 19, 0x218120,
        auxPatch = arrayOf({ textTablePatch(0x0021869C, it) })
      )
    )
    add(
      EbootTextEntry(
        81, 19, 0x218134,
        auxPatch = arrayOf({ textTablePatch(0x002186A4, it) })
      )
    )
    add(
      EbootTextEntry(
        82, 19, 0x218148,
        auxPatch = arrayOf(
          { textTablePatch(0x002186AC, it) },
          { textTablePatch(0x0021880C, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        83, 19, 0x21815C,
        auxPatch = arrayOf({ textTablePatch(0x002186B4, it) })
      )
    )
    add(
      EbootTextEntry(
        84, 19, 0x218170,
        auxPatch = arrayOf(
          { textTablePatch(0x002186BC, it) },
          { textTablePatch(0x002186EC, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        85, 19, 0x218184,
        auxPatch = arrayOf(
          { textTablePatch(0x002186C4, it) },
          { textTablePatch(0x002186F4, it) },
          { textTablePatch(0x00218814, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        86, 7, 0x218198,
        auxPatch = arrayOf({ textTablePatch(0x002186CC, it) })
      )
    )
    add(
      EbootTextEntry(
        87, 15, 0x2181A0,
        auxPatch = arrayOf(
          { textTablePatch(0x002186D4, it) },
          { textTablePatch(0x0021882C, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        88, 11, 0x2181B0,
        auxPatch = arrayOf(
          { textTablePatch(0x002186DC, it) },
          { textTablePatch(0x00218834, it) }
        )
      )
    )
    add(
      EbootTextEntry(
        89, 19, 0x2181BC,
        auxPatch = arrayOf({ textTablePatch(0x002186E4, it) })
      )
    )
    add(
      EbootTextEntry(
        90, 19, 0x2181D0,
        auxPatch = arrayOf({ textTablePatch(0x002186FC, it) })
      )
    )
    add(EbootTextEntry(91, 27, 0x2181E4))
    add(EbootTextEntry(92, 27, 0x218200))
    add(EbootTextEntry(93, 27, 0x21821C))
    add(EbootTextEntry(94, 27, 0x218238))
    add(EbootTextEntry(95, 27, 0x218254))
    add(EbootTextEntry(96, 27, 0x218270))
    add(EbootTextEntry(97, 27, 0x21828C))
    add(EbootTextEntry(98, 27, 0x2182A8))
    add(EbootTextEntry(99, 31, 0x2182C4))
    add(EbootTextEntry(100, 31, 0x2182E4))
    add(EbootTextEntry(101, 31, 0x218304))
    add(EbootTextEntry(102, 31, 0x218324))
    add(EbootTextEntry(103, 31, 0x218344))
    add(EbootTextEntry(104, 31, 0x218364))
    add(EbootTextEntry(105, 31, 0x218384))
    add(EbootTextEntry(106, 31, 0x2183A4))
    add(EbootTextEntry(107, 31, 0x2183C4))
    add(EbootTextEntry(108, 31, 0x2183E4))
    add(EbootTextEntry(109, 31, 0x218404))
    add(EbootTextEntry(110, 31, 0x218424))
    add(EbootTextEntry(111, 31, 0x218444))
    add(EbootTextEntry(112, 31, 0x218464))
    add(EbootTextEntry(113, 31, 0x218484))
    add(EbootTextEntry(114, 31, 0x2184A4))
    add(EbootTextEntry(115, 31, 0x2184C4))
    add(EbootTextEntry(116, 31, 0x2184E4))
    add(EbootTextEntry(117, 31, 0x218504))
    add(EbootTextEntry(118, 31, 0x218524))
    add(EbootTextEntry(119, 31, 0x218544))
    add(
      EbootTextEntry(
        120, 19, 0x218564,
        auxPatch = arrayOf({ textTablePatch(0x0021881C, it) })
      )
    )
    add(EbootTextEntry(121, 15, 0x218578))

    add(
      EbootTextEntry(
        122, 63, 0x002190A8,
        overrideText = "${translation.getTranslation(122)}\n${translation.getTranslation(123)}" +
          "\n${translation.getTranslation(124)}",
        auxPatch = arrayOf({ stdLoadAddress(a0, 0x001A1248, 0x001A1250, it) })
      )
    )

    add(
      EbootTextEntry(
        125, 39, 0x002190E8,
        overrideText = "${translation.getTranslation(125)}\n${translation.getTranslation(126)}",
        auxPatch = arrayOf({ stdLoadAddress(a1, 0x001A1254, 0x001A125C, it) })
      )
    )

    add(
      EbootTextEntry(
        127, 27, 0x00219110,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A1260, it) })
      )
    )

    add(EbootTextEntry(123, -1, -1, skip = true))
    add(EbootTextEntry(124, -1, -1, skip = true))
    add(EbootTextEntry(126, -1, -1, skip = true))

    add(
      EbootTextEntry(
        128, 3, 0x21912C,
        auxPatch = arrayOf({ stdLoadAddress(a1, 0x001A126C, 0x001A1274, it) })
      )
    )
    add(
      EbootTextEntry(
        129, 3, 0x219130,
        auxPatch = arrayOf({ stdLoadAddress(a0, 0x001A1278, 0x001A1280, it) })
      )
    )
    add(EbootTextEntry(130, 11, 0x219134))
    add(EbootTextEntry(131, 11, 0x219140))
    add(EbootTextEntry(132, 11, 0x21914C))
    add(EbootTextEntry(133, 15, 0x219158))
    add(EbootTextEntry(134, 15, 0x219168))

    add(EbootTextEntry(135, 7, 0x219190, overrideText = "%s %s"))
    add(EbootTextEntry(136, 11, 0x219198, overrideText = "%s %s ★"))
    add(EbootTextEntry(137, 3, 0x2191A4, skip = true))
    add(EbootTextEntry(138, 7, 0x2191A8, overrideText = "%s ★"))

    add(EbootTextEntry(139, 11, 0x2191B0))
    add(
      EbootTextEntry(
        140, 7, 0x2191BC,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A176C, it) })
      )
    )
    add(
      EbootTextEntry(
        141, 7, 0x2191C4,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A1778, it) })
      )
    )
    add(
      EbootTextEntry(
        142, 7, 0x2191CC,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A1784, it) })
      )
    )
    add(
      EbootTextEntry(
        143, 7, 0x2191D4,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A1790, it) })
      )
    )
    add(
      EbootTextEntry(
        144, 7, 0x2191DC,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A179C, it) })
      )
    )
    add(
      EbootTextEntry(
        145, 7, 0x2191E4,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A17A8, it) })
      )
    )
    add(
      EbootTextEntry(
        146, 7, 0x2191EC,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A17B4, it) })
      )
    )
    add(
      EbootTextEntry(
        147, 7, 0x2191F4,
        auxPatch = arrayOf({ stdLoadAddressImm(a0, 0x001A17C0, it) })
      )
    )
    add(EbootTextEntry(148, 15, 0x219200))
    add(
      EbootTextEntry(
        149, 7, 0x21B340,
        auxPatch = arrayOf({ textTablePatch(0x0021B504, it) })
      )
    )
    add(EbootTextEntry(150, 7, 0x21B348))
    add(
      EbootTextEntry(
        151, 11, 0x21B350,
        auxPatch = arrayOf({ textTablePatch(0x0021B514, it) })
      )
    )
    add(EbootTextEntry(152, 7, 0x21B35C))
    add(EbootTextEntry(153, 7, 0x21B364))
    add(EbootTextEntry(154, 7, 0x21B36C))
    add(EbootTextEntry(155, 7, 0x21B374))
    add(EbootTextEntry(156, 11, 0x21B37C))
    add(EbootTextEntry(157, 7, 0x21B388))
    add(
      EbootTextEntry(
        158, 11, 0x21B390,
        auxPatch = arrayOf({ textTablePatch(0x0021B54C, it) })
      )
    )
    add(
      EbootTextEntry(
        159, 7, 0x21B39C,
        auxPatch = arrayOf({ textTablePatch(0x0021B554, it) })
      )
    )
    add(
      EbootTextEntry(
        160, 11, 0x21B3A4,
        auxPatch = arrayOf({ textTablePatch(0x0021B55C, it) })
      )
    )
    add(
      EbootTextEntry(
        161, 11, 0x21B3B0,
        auxPatch = arrayOf({ textTablePatch(0x0021B564, it) })
      )
    )
    add(
      EbootTextEntry(
        162, 11, 0x21B3BC,
        auxPatch = arrayOf({ textTablePatch(0x0021B56C, it) })
      )
    )
    add(
      EbootTextEntry(
        163, 11, 0x21B3C8,
        auxPatch = arrayOf({ textTablePatch(0x0021B574, it) })
      )
    )
    add(EbootTextEntry(164, 7, 0x1B5BF4))
    add(EbootTextEntry(164, 7, 0x21B3D4))
    add(EbootTextEntry(165, 15, 0x21B3DC))
    add(EbootTextEntry(166, 11, 0x21B3EC))
    add(EbootTextEntry(167, 11, 0x21B3F8))
    add(EbootTextEntry(168, 11, 0x21B404))
    add(EbootTextEntry(169, 11, 0x21B410))
    add(EbootTextEntry(170, 15, 0x21B41C))
    add(EbootTextEntry(171, 7, 0x21B42C))
    add(EbootTextEntry(172, 15, 0x21B434))
    add(EbootTextEntry(173, 15, 0x21B444))
    add(EbootTextEntry(174, 15, 0x21B454))
    add(EbootTextEntry(175, 15, 0x21B464))
    add(EbootTextEntry(176, 7, 0x21B474))
    add(EbootTextEntry(177, 19, 0x21B47C))
    add(EbootTextEntry(178, 11, 0x21B490))
    add(EbootTextEntry(179, 15, 0x21B49C))
    add(EbootTextEntry(180, 7, 0x21B4AC))
    add(EbootTextEntry(181, 11, 0x21B4B4))
    add(EbootTextEntry(182, 11, 0x21B4C0))
    add(
      EbootTextEntry(
        183, 11, 0x21B4CC,
        auxPatch = arrayOf({ textTablePatch(0x0021B614, it) })
      )
    )
    add(EbootTextEntry(184, 19, 0x21B4D8))
    add(EbootTextEntry(185, 15, 0x21B4EC))
  }
  return entries
}

class EbootTextEntry(
  val tlIndex: Int,
  val allowedSize: Int,
  val address: Int,
  val skip: Boolean = false,
  val overrideText: String = "",
  val auxPatch: Array<(insertedTextAddr: Int) -> ElfPatch> = emptyArray(),
) {
  val physicalAddress = if (address == -1) -1 else address + 0xA0
  val auxPatchUsed = !auxPatch.contentEquals(emptyArray())
}
