"""Generates the PNG textures for the Enchantment Forge mod (no Pillow needed).

Usage: python tools/gen_textures.py <project root>
Slot positions in gui() must match EnchantmentForgeMenu / EnchantmentForgeScreen.
"""
import math
import os
import random
import struct
import sys
import zlib


def write_png(path, w, h, px):
    """px: list of rows, each row a list of (r, g, b, a)."""
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            raw.extend(px[y][x])

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def canvas(w, h, color=(0, 0, 0, 0)):
    return [[color for _ in range(w)] for _ in range(h)]


# ---------------------------------------------------------------- block textures
OBS = [(16, 10, 27, 255), (24, 15, 38, 255), (33, 21, 52, 255), (45, 30, 70, 255), (60, 40, 92, 255)]
GLOW = [(120, 60, 190, 255), (165, 100, 240, 255), (215, 165, 255, 255)]
METAL = [(52, 52, 56, 255), (72, 72, 78, 255), (98, 98, 104, 255), (128, 128, 134, 255), (160, 160, 166, 255)]
DIA = [(40, 150, 150, 255), (90, 220, 210, 255), (180, 255, 245, 255)]


def obsidian(seed):
    rnd = random.Random(seed)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            v = rnd.random()
            idx = 0 if v < 0.25 else 1 if v < 0.6 else 2 if v < 0.88 else 3 if v < 0.97 else 4
            px[y][x] = OBS[idx]
    return px


def frame(px, x0, y0, x1, y1, light, dark):
    for x in range(x0, x1 + 1):
        px[y0][x] = light
        px[y1][x] = dark
    for y in range(y0, y1 + 1):
        px[y][x0] = light
        px[y][x1] = dark


def gem(px, cx, cy):
    px[cy][cx] = DIA[2]
    px[cy][cx + 1] = DIA[1]
    px[cy + 1][cx] = DIA[1]
    px[cy + 1][cx + 1] = DIA[0]


def block_side():
    px = obsidian(11)
    # metal cap band (the anvil-like top lip)
    for y in range(0, 3):
        for x in range(16):
            px[y][x] = METAL[3 - y]
    for x in range(16):
        px[3][x] = METAL[0]
    for x in range(0, 16, 4):
        px[1][x] = METAL[4]
    # rivets at the bottom band
    for x in range(16):
        px[15][x] = METAL[1]
        px[14][x] = METAL[2] if x % 5 else METAL[4]
    # glowing rune in the middle
    rune = [
        "....#....",
        "...#.#...",
        "..#...#..",
        ".#..#..#.",
        "#..###..#",
        ".#..#..#.",
        "..#...#..",
        "...#.#...",
        "....#....",
    ]
    for ry, row in enumerate(rune):
        for rx, c in enumerate(row):
            if c == "#":
                x, y = 3 + rx, 4 + ry
                core = abs(rx - 4) + abs(ry - 4) <= 1
                px[y][x] = GLOW[2] if core else GLOW[1]
    # soft halo around the rune
    for ry in range(len(rune)):
        for rx in range(len(rune[0])):
            x, y = 3 + rx, 4 + ry
            if px[y][x] in OBS and abs(rx - 4) + abs(ry - 4) <= 4:
                r, g, b, a = px[y][x]
                px[y][x] = (min(255, r + 18), min(255, g + 6), min(255, b + 30), 255)
    gem(px, 0, 12)
    gem(px, 14, 12)
    return px


def block_top():
    px = canvas(16, 16)
    rnd = random.Random(7)
    for y in range(16):
        for x in range(16):
            px[y][x] = METAL[2] if rnd.random() < 0.7 else METAL[1]
    frame(px, 0, 0, 15, 15, METAL[4], METAL[0])
    frame(px, 1, 1, 14, 14, METAL[3], METAL[1])
    # obsidian inlay
    for y in range(3, 13):
        for x in range(3, 13):
            px[y][x] = OBS[rnd.choice([0, 1, 1, 2])]
    frame(px, 3, 3, 12, 12, METAL[0], METAL[3])
    # glowing ring
    ring = [(6, 4), (7, 4), (8, 4), (9, 4), (5, 5), (10, 5), (4, 6), (11, 6), (4, 7), (11, 7),
            (4, 8), (11, 8), (4, 9), (11, 9), (5, 10), (10, 10), (6, 11), (7, 11), (8, 11), (9, 11)]
    for x, y in ring:
        px[y][x] = GLOW[1]
    for x, y in [(7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)]:
        px[y][x] = GLOW[0]
    for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[y][x] = GLOW[2]
    for cx, cy in [(1, 1), (13, 1), (1, 13), (13, 13)]:
        gem(px, cx, cy)
    return px


def block_bottom():
    px = obsidian(23)
    frame(px, 0, 0, 15, 15, METAL[2], METAL[0])
    return px


# ---------------------------------------------------------------- item textures
BONE = [(250, 247, 230, 255), (226, 219, 190, 255), (184, 174, 140, 255)]
IRON = [(236, 236, 240, 255), (196, 196, 204, 255), (138, 138, 148, 255)]
OUTLINE = (52, 44, 38, 255)


def luck_charm():
    """A bone bound by an iron band that holds a purple gem, drawn from simple shapes."""
    px = canvas(16, 16)
    ax, ay, bx, by = 4.0, 12.0, 12.0, 4.0  # bone axis, bottom-left to top-right
    dx, dy = bx - ax, by - ay
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length  # along the bone
    nx, ny = uy, -ux  # across the bone; positive side faces the upper left
    mid = length / 2

    def coords(x, y):
        rx, ry = x + 0.5 - ax, y + 0.5 - ay
        return rx * ux + ry * uy, rx * nx + ry * ny  # (along, across)

    for y in range(16):
        for x in range(16):
            along, across = coords(x, y)
            band = abs(along - mid) <= 1.9 and abs(across) <= 2.2
            knob = any(math.hypot(along - end, across - side) <= 1.75
                       for end in (-0.3, length + 0.3) for side in (-1.45, 1.45))
            shaft = -0.3 <= along <= length + 0.3 and abs(across) <= 1.4
            if band:
                px[y][x] = IRON[0] if across > 0.9 else IRON[2] if across < -0.9 else IRON[1]
            elif knob or shaft:
                px[y][x] = BONE[0] if across > 0.6 else BONE[2] if across < -0.7 else BONE[1]
    # gem in the middle of the band
    cx, cy = int(ax + dx / 2), int(ay + dy / 2)
    px[cy - 1][cx - 1] = GLOW[2]
    px[cy - 1][cx] = GLOW[1]
    px[cy][cx - 1] = GLOW[1]
    px[cy][cx] = GLOW[0]
    # dark outline around everything
    filled = [[px[y][x][3] > 0 for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if filled[y][x]:
                continue
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + ox < 16 and 0 <= y + oy < 16 and filled[y + oy][x + ox]:
                    px[y][x] = OUTLINE
                    break
    return px


# ---------------------------------------------------------------- GUI texture
WHITE = (255, 255, 255, 255)
BG = (198, 198, 198, 255)
SHADOW = (85, 85, 85, 255)
BLACK = (0, 0, 0, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_BG = (139, 139, 139, 255)
CHARM_SLOT_BG = (134, 122, 156, 255)


def rect(px, x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[yy][xx] = c


def panel(px, w, h):
    rect(px, 1, 1, w - 2, h - 2, BG)
    # black outline with cut corners
    for x in range(2, w - 2):
        px[0][x] = BLACK
        px[h - 1][x] = BLACK
    for y in range(2, h - 2):
        px[y][0] = BLACK
        px[y][w - 1] = BLACK
    for x, y in [(1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)]:
        px[y][x] = BLACK
    # bevel
    rect(px, 2, 1, w - 5, 2, WHITE)
    rect(px, 1, 2, 2, h - 5, WHITE)
    rect(px, 3, h - 3, w - 5, 2, SHADOW)
    rect(px, w - 3, 3, 2, h - 5, SHADOW)
    px[3][3] = WHITE
    px[h - 4][w - 4] = SHADOW
    # corner pixels where the light and dark bevel meet
    px[1][w - 3] = BG
    px[2][w - 3] = BG
    px[h - 3][1] = BG
    px[h - 3][2] = BG
    px[h - 2][1] = (0, 0, 0, 0)
    px[1][w - 2] = BLACK
    px[h - 2][w - 2] = BLACK
    # transparent outer corners
    for x, y in [(0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1),
                 (0, h - 1), (1, h - 1), (0, h - 2), (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)]:
        px[y][x] = (0, 0, 0, 0)
    px[h - 2][1] = BLACK


def inset(px, x, y, w, h, fill=SLOT_BG):
    """Sunken box with outer size w*h at (x, y)."""
    rect(px, x, y, w, h, fill)
    for xx in range(x, x + w - 1):
        px[y][xx] = SLOT_DARK
    for yy in range(y, y + h - 1):
        px[yy][x] = SLOT_DARK
    for xx in range(x + 1, x + w):
        px[y + h - 1][xx] = WHITE
    for yy in range(y + 1, y + h):
        px[yy][x + w - 1] = WHITE


def slot(px, sx, sy):
    inset(px, sx - 1, sy - 1, 18, 18)


def plus(px, cx, cy, c):
    for d in range(-3, 4):
        px[cy][cx + d] = c
        px[cy + d][cx] = c
        px[cy + 1][cx + d] = c
        px[cy + d][cx + 1] = c


def clover(px, cx, cy):
    leaf = (70, 160, 60, 255)
    light = (120, 205, 95, 255)
    for ox, oy in ((-2, -2), (1, -2), (-2, 1), (1, 1)):
        rect(px, cx + ox, cy + oy, 2, 2, leaf)
        px[cy + oy][cx + ox] = light
    px[cy + 3][cx] = (60, 120, 50, 255)
    px[cy + 4][cx + 1] = (60, 120, 50, 255)


def scroller(px, x, y, enabled):
    light = WHITE if enabled else (230, 230, 230, 255)
    mid = BG if enabled else (160, 160, 160, 255)
    dark = SHADOW if enabled else (110, 110, 110, 255)
    rect(px, x, y, 12, 15, mid)
    for xx in range(x, x + 12):
        px[y][xx] = light
        px[y + 14][xx] = dark
    for yy in range(y, y + 15):
        px[yy][x] = light
        px[yy][x + 11] = dark
    for i in range(3):
        for xx in range(x + 3, x + 9):
            px[y + 5 + i * 2][xx] = dark


def gui():
    W = H = 256
    px = canvas(W, H)
    panel_w, panel_h = 176, 222
    sub = canvas(panel_w, panel_h)
    panel(sub, panel_w, panel_h)
    # forge slots: item + material, then the tinted Luck Charm slot (positions as in EnchantmentForgeMenu)
    slot(sub, 17, 24)
    slot(sub, 17, 60)
    plus(sub, 24, 49, (110, 60, 170, 255))
    clover(sub, 24, 85)
    inset(sub, 16, 97, 18, 18, CHARM_SLOT_BG)
    # enchantment list frame and scrollbar track
    inset(sub, 41, 17, 114, 102, (60, 46, 78, 255))
    inset(sub, 156, 17, 14, 102)
    # player inventory
    for row in range(3):
        for col in range(9):
            slot(sub, 8 + col * 18, 140 + row * 18)
    for col in range(9):
        slot(sub, 8 + col * 18, 198)
    for y in range(panel_h):
        for x in range(panel_w):
            px[y][x] = sub[y][x]
    scroller(px, 176, 0, True)
    scroller(px, 188, 0, False)
    return px


def main(root):
    tex = os.path.join(root, "src", "main", "resources", "assets", "enchantforge", "textures")
    write_png(os.path.join(tex, "block", "enchantment_forge_side.png"), 16, 16, block_side())
    write_png(os.path.join(tex, "block", "enchantment_forge_top.png"), 16, 16, block_top())
    write_png(os.path.join(tex, "block", "enchantment_forge_bottom.png"), 16, 16, block_bottom())
    write_png(os.path.join(tex, "gui", "container", "enchantment_forge.png"), 256, 256, gui())
    write_png(os.path.join(tex, "item", "luck_charm.png"), 16, 16, luck_charm())
    # mod icon: upscale of the side texture
    side = block_side()
    icon = [[side[y // 8][x // 8] for x in range(128)] for y in range(128)]
    write_png(os.path.join(root, "src", "main", "resources", "assets", "enchantforge", "icon.png"), 128, 128, icon)


if __name__ == "__main__":
    main(sys.argv[1])
