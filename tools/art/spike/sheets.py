"""Contact sheets: one per direction (4 views) and an overview of the three front views."""
from PIL import Image, ImageDraw, ImageFont

NAMES = {'A': 'A - Bulkhead', 'B': 'B - Scavenger', 'C': 'C - Boiler Back'}
VIEWS = [('full_front', 'full set, front 3/4'), ('full_back', 'full set, back 3/4'),
         ('full_side', 'full set, side (right)'), ('frame_front', 'bare frame, front 3/4')]
T = 700  # tile size
BG = (128, 128, 128)


def font(sz):
    for p in ('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
              '/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf'):
        try:
            return ImageFont.truetype(p, sz)
        except OSError:
            pass
    return ImageFont.load_default()


def sheet(tiles, title, path):
    head = 70
    img = Image.new('RGB', (T * len(tiles), T + head + 40), (96, 96, 96))
    d = ImageDraw.Draw(img)
    d.text((20, 18), title, fill=(240, 240, 240), font=font(34))
    for k, (file, label) in enumerate(tiles):
        im = Image.open(file).convert('RGB').resize((T, T), Image.LANCZOS)
        img.paste(im, (k * T, head))
        d.text((k * T + 16, head + T + 6), label, fill=(235, 235, 235), font=font(24))
    img.save(path)


for k, n in NAMES.items():
    sheet([(f'out/{k}_{v}.png', lab) for v, lab in VIEWS], f'Rottame (Scrap) - {n}', f'out/{k}_sheet.png')
sheet([(f'out/{k}_full_front.png', NAMES[k]) for k in NAMES],
      'Rottame (Scrap) - three directions, front 3/4', 'out/overview.png')
print('sheets written')
