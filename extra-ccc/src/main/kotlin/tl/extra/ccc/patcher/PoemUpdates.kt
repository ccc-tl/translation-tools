package tl.extra.ccc.patcher

fun getPoemUpdates(): Map<String, List<PoemUpdate>> {
  return mapOf(
    "poem_00" to listOf(
      PoemUpdate.TextY(0x3, 0xC, -22), // l1,2
      PoemUpdate.TextY(0x4, 0xD, 3), // l3
      PoemUpdate.TextY(0x5, 0xE, 3), // l4
      PoemUpdate.TextY(0x6, 0xF, 14), // l5
      PoemUpdate.TextY(0x7, 0x10, 25), // l6,7

      PoemUpdate.Light(0, 0x9, -105f, 148f, -22f),
      PoemUpdate.Light(1, 0x9, -95f, 148f, 3f),
      PoemUpdate.Light(2, 0x9, -160f, 180f, 3f),
      PoemUpdate.Light(3, 0x9, -125f, 165f, 14f),
      PoemUpdate.Light(4, 0x9, -125f, 145f, 25f),
    ),
    "poem_01" to listOf(
      // s1
      PoemUpdate.TextY(0x3, 0xC, -17), // l1

      // s2
      PoemUpdate.TextY(0x5, 0xE, -23), // l1
      PoemUpdate.TextY(0x6, 0xF, -11), // l2

      PoemUpdate.TextY(0x7, 0x10, -10), // l3
      PoemUpdate.TextY(0x8, 0x11, -9), // l4

      PoemUpdate.TextY(0x9, 0x12, -17), // l5
      PoemUpdate.TextY(0xA, 0x13, -8), // l6,7

      PoemUpdate.TextY(0xB, 0x14, 11), // l8

      // s1 lights
      PoemUpdate.Light(0, 0x14, -110f, 125f, -17f),
      PoemUpdate.Light(1, 0x14, -95f, 132f, 0f),

      // s2 lights
      PoemUpdate.Light(2, 0x14, -80f, 115f, -23f),
      PoemUpdate.Light(3, 0x14, -120f, 145f, -11f),

      PoemUpdate.Light(4, 0x14, -110f, 143f, -10f),
      PoemUpdate.Light(5, 0x14, -115f, 144f, -9f),

      PoemUpdate.Light(6, 0x14, -120f, 134f, -17f),
      PoemUpdate.Light(7, 0x14, -150f, 177f, -8f),

      PoemUpdate.Light(8, 0x14, -180f, 205f, 11f),
    ),
    "poem_02" to listOf(
      // s1
      PoemUpdate.TextY(0x3, -1, -15), // l1
      PoemUpdate.TextY(0x4, -1, -5), // l2

      // s2
      PoemUpdate.TextY(0x5, -1, -20), // l1

      PoemUpdate.TextX(0x6, -1, -5),
      PoemUpdate.TextY(0x6, -1, -5), // l2p1
      PoemUpdate.TextX(0x7, -1, 15),
      PoemUpdate.TextY(0x7, -1, -3), // l2p2

      PoemUpdate.TextX(0x8, -1, -5),
      PoemUpdate.TextY(0x8, -1, -5), // l3p1
      PoemUpdate.TextX(0x9, -1, 20),
      PoemUpdate.TextY(0x9, -1, -3), // l3p2

      PoemUpdate.TextX(0xA, -1, -25),
      PoemUpdate.TextY(0xA, -1, 10), // l4,5p2
      PoemUpdate.TextX(0xB, -1, 10),
      PoemUpdate.TextY(0xB, -1, 12), // l4,5p2

      PoemUpdate.TextX(0x15, -1, -16),
      PoemUpdate.TextY(0x15, -1, 25), // l5p1
      PoemUpdate.TextX(0x17, -1, 27),
      PoemUpdate.TextY(0x17, -1, 26), // l5p2

      // s1
      PoemUpdate.Light(0, 0x14, -100f, 136f, -15f),
      PoemUpdate.Light(1, 0x14, -100f, 125f, -5f),

      // s2
      PoemUpdate.Light(2, 0x14, -145f, 170f, -20f),
      PoemUpdate.Light(3, 0x14, -174f, 205f, -5f),
      PoemUpdate.Light(4, 0x14, -180f, 210f, -5f),
      PoemUpdate.Light(5, 0x14, -176f, 155f, 10f),
      PoemUpdate.Light(6, 0x14, -185f, 207f, 25f),
    ),
    "poem_03" to listOf(
      // s1
      PoemUpdate.TextY(0x3, -1, -10), // l1
      PoemUpdate.TextY(0x4, -1, 20), // l2

      PoemUpdate.TextY(0x17, -1, -15), // ruby l1
      PoemUpdate.TextY(0x18, -1, 16), // ruby l2

      // s2
      PoemUpdate.TextY(0x5, -1, -5), // l1
      PoemUpdate.TextY(0x7, -1, 2), // l3
      PoemUpdate.TextY(0x8, -1, 4), // l4

      // s3
      PoemUpdate.TextY(0x9, -1, -30), // l1,2
      PoemUpdate.TextY(0xA, -1, -5), // l3,4
      PoemUpdate.TextY(0xB, -1, -10), // l5
      PoemUpdate.TextY(0x15, -1, 25), // l6,7

      PoemUpdate.TextX(0x19, -1, -140), // ruby l3
      PoemUpdate.TextY(0x19, -1, 12), // ruby l3

      // s1 lights
      PoemUpdate.Light(0, 0x19, -115f, 138f, -10f),
      PoemUpdate.Light(1, 0x19, -105f, 140f, 20f),

      // s2 lights
      PoemUpdate.Light(2, 0x19, -155f, 196f, -5f),
      PoemUpdate.Light(3, 0x19, -125f, 146f, 0f),
      PoemUpdate.Light(4, 0x19, -165f, 195f, 2f),
      PoemUpdate.Light(5, 0x19, -60f, 85f, 4f),

      // s3 lights
      PoemUpdate.Light(6, 0x19, -90f, 125f, -30f),
      PoemUpdate.Light(7, 0x19, -160f, 210f, -5f),
      PoemUpdate.Light(8, 0x19, -185f, 235f, -10f),
      PoemUpdate.Light(9, 0x19, -130f, 155f, 25f),
    ),
    "poem_04" to listOf(
      // s1
      PoemUpdate.TextY(0x3, -1, -5), // l1
      PoemUpdate.TextY(0x4, -1, 5), // l2

      // s2
      PoemUpdate.TextY(0x5, -1, -15), // l1
      PoemUpdate.TextY(0x6, -1, 3), // l2
      PoemUpdate.TextY(0x7, -1, 5), // l3
      PoemUpdate.TextY(0x8, -1, 13), // l4
      PoemUpdate.TextY(0x9, -1, 15), // l5

      PoemUpdate.TextX(0x13, -1, 80), // ruby X
      PoemUpdate.TextY(0x13, -1, -13), // ruby Y

      // s1 lights
      PoemUpdate.Light(0, 0xE, -100f, 118f, -5f),
      PoemUpdate.Light(1, 0xE, -160f, 200f, 5f),

      // s2 lights
      PoemUpdate.Light(2, 0xE, -135f, 165f, -15f),
      PoemUpdate.Light(3, 0xE, -175f, 190f, 3f),
      PoemUpdate.Light(4, 0xE, -180f, 192f, 5f),
      PoemUpdate.Light(5, 0xE, -170f, 195f, 13f),
      PoemUpdate.Light(6, 0x13, -170f, 190f, 15f),

      // swap ruby to be linked to appearance of s2l4 instead of s2l5
      PoemUpdate.SwapBytes(
        5, 0x904, 0x34,
        6, 0x96C, 6 * 0x34,
      ),
      PoemUpdate.WriteMobInt(5, 0xA8, 6),
      PoemUpdate.WriteMobInt(6, 0xA8, 1),
      PoemUpdate.WriteMobInt(6, 0x96C + 0x28, -1), // set color1 to fully visible
    ),
    "poem_05" to listOf(
      // s1
      PoemUpdate.TextY(0x3, -1, -17), // l1
      PoemUpdate.TextY(0x4, -1, 5), // l2,3

      // s2
      PoemUpdate.TextY(0x5, -1, -33), // l1
      PoemUpdate.TextY(0x6, -1, -33), // l2

      PoemUpdate.TextY(0x7, -1, -35), // l3
      PoemUpdate.TextY(0x8, -1, -31), // l4

      PoemUpdate.TextY(0x9, -1, 1), // l5,6

      PoemUpdate.TextY(0xA, -1, 12), // l7
      PoemUpdate.TextY(0xB, -1, 22), // l8,9

      PoemUpdate.TextX(0x15, -1, -46), // ruby 1
      PoemUpdate.TextY(0x15, -1, -9),
      PoemUpdate.TextX(0x16, -1, -55), // ruby 2
      PoemUpdate.TextY(0x16, -1, 35),

      // s1 lights
      PoemUpdate.Light(0, 0x14, -110f, 128f, -17f),
      PoemUpdate.Light(1, 0x14, -140f, 180f, 5f),

      // s2 lights
      PoemUpdate.Light(2, 0x14, -88f, 110f, -33f),
      PoemUpdate.Light(3, 0x14, -88f, 110f, -33f),

      PoemUpdate.Light(4, 0x14, -130f, 150f, -35f),
      PoemUpdate.Light(5, 0x14, -165f, 168f, -31f),

      PoemUpdate.Light(6, 0x14, -125f, 155f, 1f),

      PoemUpdate.Light(7, 0x14, -120f, 130f, 12f),
      PoemUpdate.Light(8, 0x14, -115f, 135f, 22f),
    ),
    "poem_06" to listOf(
      // s1
      PoemUpdate.TextY(0x1F, -1, -13), // ruby l1
      PoemUpdate.TextY(0x3, -1, -10), // l1

      PoemUpdate.TextY(0x20, -1, 17), // ruby l2
      PoemUpdate.TextY(0x4, -1, 20), // l2

      // s2
      PoemUpdate.TextY(0x5, -1, -10), // l1
      PoemUpdate.TextY(0x6, -1, -10), // l2
      PoemUpdate.TextY(0x7, -1, 0), // l3
      PoemUpdate.TextY(0x8, -1, 0), // l4

      // s3
      PoemUpdate.TextY(0x9, -1, -11), // l1
      PoemUpdate.TextY(0xA, -1, -9), // l2,3
      PoemUpdate.TextY(0xB, -1, 14), // l4
      PoemUpdate.TextY(0x15, -1, 14), // l5

      // s4
      PoemUpdate.TextY(0x17, -1, -17), // l1
      PoemUpdate.TextY(0x19, -1, -15), // l2
      PoemUpdate.TextY(0x1B, -1, 5), // l3
      PoemUpdate.TextY(0x1D, -1, 16), // l4,5

      // s1 lights
      PoemUpdate.Light(0, 0x1E, -95f, 130f, -10f),
      PoemUpdate.Light(1, 0x1E, -115f, 145f, 20f),

      // s2 lights
      PoemUpdate.Light(2, 0x1E, -115f, 145f, -10f),
      PoemUpdate.Light(3, 0x1E, -125f, 165f, -10f),
      PoemUpdate.Light(4, 0x1E, -95f, 127f, 0f),
      PoemUpdate.Light(5, 0x1E, -85f, 110f, 0f),

      // s3 lights
      PoemUpdate.Light(6, 0x1E, -75f, 105f, -11f),
      PoemUpdate.Light(7, 0x1E, -155f, 185f, -9f),
      PoemUpdate.Light(8, 0x1E, -105f, 140f, 14f),
      PoemUpdate.Light(9, 0x1E, -95f, 125f, 14f),

      // s4 lights
      PoemUpdate.Light(10, 0x1E, -135f, 185f, -17f),
      PoemUpdate.Light(11, 0x1E, -170f, 205f, -15f),
      PoemUpdate.Light(12, 0x1E, -155f, 180f, 5f),
      PoemUpdate.Light(13, 0x1E, -95f, 130f, 15f),
    ),
    "poem_07" to listOf(
      // s1
      PoemUpdate.TextY(0x3, -1, -14), // l1
      PoemUpdate.TextY(0x4, -1, 0), // l2,3

      PoemUpdate.TextX(0x5, -1, -148), // ruby l3
      PoemUpdate.TextY(0x5, -1, 13),

      // s1 lights
      PoemUpdate.Light(0, 0xD, -95f, 125f, -14f),
      PoemUpdate.Light(1, 0x5, -105f, 138f, 0f),
    ),
    "poem_08" to listOf(
      // s1
      PoemUpdate.TextY(0x3, -1, -14), // l1
      PoemUpdate.TextY(0x4, -1, 0), // l2,3

      PoemUpdate.TextX(0x5, -1, -178), // ruby l3
      PoemUpdate.TextY(0x5, -1, 12),

      // s1 lights
      PoemUpdate.Light(0, 0xD, -175f, 190f, -14f),
      PoemUpdate.Light(1, 0x5, -115f, 135f, 0f),
    ),
    "poem_ex" to listOf(
      // s1
      PoemUpdate.TextY(0x1, 0x2, -20), // l1

      PoemUpdate.TextY(0x3, 0x4, -18), // l2
      PoemUpdate.TextY(0x5, 0x6, -18), // l3

      PoemUpdate.TextY(0x7, 0x8, -7), // l4,5

      PoemUpdate.TextY(0x9, 0xA, 19), // l6
      PoemUpdate.TextY(0xB, 0xC, 19), // l7

      // s2
      PoemUpdate.TextY(0xD, 0xE, -27), // l1
      PoemUpdate.TextY(0xF, 0x10, -13), // l2,3

      PoemUpdate.TextY(0x11, 0x12, 21), // l4,5
      PoemUpdate.TextY(0x13, 0x14, 28), // l5

      // s1 lights
      PoemUpdate.Light(0, 1, -25f, 100f, -20f, 6 + 0),
      PoemUpdate.Light(0, 1, -110f, 190f, -18f, 6 + 4),
      PoemUpdate.Light(0, 1, -110f, 180f, -18f, 6 + 8),
      PoemUpdate.Light(0, 1, -107f, 195f, -7f, 6 + 12),
      PoemUpdate.Light(0, 1, -62f, 140f, 19f, 6 + 16),
      PoemUpdate.Light(0, 1, -95f, 175f, 19f, 6 + 20),

      // s2 lights
      PoemUpdate.Light(0, 1, -90f, 180f, -27f, 6 + 24),
      PoemUpdate.Light(0, 1, -145f, 240f, -13f, 6 + 28),
      PoemUpdate.Light(0, 1, -145f, 230f, 21f, 6 + 32),
      PoemUpdate.Light(0, 1, -125f, 192f, 28f, 6 + 36),
    ),
  )
}
