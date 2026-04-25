import sharp from 'sharp';
import fs from 'fs';

const sizes = {
  'mipmap-mdpi': 48,
  'mipmap-hdpi': 72,
  'mipmap-xhdpi': 96,
  'mipmap-xxhdpi': 144,
  'mipmap-xxxhdpi': 192,
};

const base = 'android/app/src/main/res';

for (const [folder, size] of Object.entries(sizes)) {
  const dir = `${base}/${folder}`;
  fs.mkdirSync(dir, {recursive: true});

  const logoSize = Math.round(size * 1);
  const padding = Math.round((size - logoSize) / 2);

  // Icono normal
  await sharp({
    create: {
      width: size,
      height: size,
      channels: 4,
      background: {r: 0, g: 0, b: 0, alpha: 1},
    },
  })
    .composite([
      {
        input: await sharp('assets/artwork.png')
          .resize(logoSize, logoSize, {
            fit: 'contain',
            background: {r: 0, g: 0, b: 0, alpha: 0},
          })
          .toBuffer(),
        top: padding,
        left: padding,
      },
    ])
    .png()
    .toFile(`${dir}/ic_launcher.png`);

  // Foreground adaptativo: el canvas es 108dp pero el safe zone es 72dp (66%)
  // Para que el logo no se corte hay que que ocupe como máximo el 40% del canvas
  const adaptiveSize = Math.round(size * 1.5); // canvas más grande (108dp equiv)
  const adaptiveLogoSize = Math.round(adaptiveSize * 0.5); // logo al 38% del canvas
  const adaptivePadding = Math.round((adaptiveSize - adaptiveLogoSize) / 2);

  await sharp({
    create: {
      width: adaptiveSize,
      height: adaptiveSize,
      channels: 4,
      background: {r: 0, g: 0, b: 0, alpha: 0},
    },
  })
    .composite([
      {
        input: await sharp('assets/artwork.png')
          .resize(adaptiveLogoSize, adaptiveLogoSize, {
            fit: 'contain',
            background: {r: 0, g: 0, b: 0, alpha: 0},
          })
          .toBuffer(),
        top: adaptivePadding,
        left: adaptivePadding,
      },
    ])
    .png()
    .toFile(`${dir}/ic_launcher_foreground.png`);

  // Icono redondo
  const circle = Buffer.from(
    `<svg><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}"/></svg>`,
  );

  await sharp({
    create: {
      width: size,
      height: size,
      channels: 4,
      background: {r: 0, g: 0, b: 0, alpha: 1},
    },
  })
    .composite([
      {
        input: await sharp('assets/artwork.png')
          .resize(logoSize, logoSize, {
            fit: 'contain',
            background: {r: 0, g: 0, b: 0, alpha: 0},
          })
          .toBuffer(),
        top: padding,
        left: padding,
      },
    ])
    .composite([{input: circle, blend: 'dest-in'}])
    .png()
    .toFile(`${dir}/ic_launcher_round.png`);

  console.log(`✓ ${folder} (${size}px)`);
}

const colorsPath = 'android/app/src/main/res/values/colors.xml';
const colorsContent = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#000000</color>
</resources>`;
fs.mkdirSync('android/app/src/main/res/values', {recursive: true});
fs.writeFileSync(colorsPath, colorsContent);
console.log('✓ colors.xml actualizado con fondo negro');
console.log('✓ Iconos generados correctamente');
