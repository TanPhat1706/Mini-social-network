/**
 * 🥚 Easter Egg Combo Detector — World Cup 2026
 *
 * Khi người dùng trang bị đồng thời Viền Quốc Gia + Màu Tên của cùng một đội,
 * hàm này phát hiện combo và trả về tên quốc gia để áp `data-combo` attribute.
 *
 * CSS sẽ dùng [data-combo="vietnam"] { ... } để render hiệu ứng hào quang.
 */

/** Danh sách 13 combo quốc gia hợp lệ */
const NATIONAL_COMBOS: Record<string, { frame: string; color: string }> = {
  vietnam:     { frame: 'css-frame-wc-vietnam',     color: 'css-color-wc-vietnam'     },
  brazil:      { frame: 'css-frame-wc-brazil',      color: 'css-color-wc-brazil'      },
  argentina:   { frame: 'css-frame-wc-argentina',   color: 'css-color-wc-argentina'   },
  france:      { frame: 'css-frame-wc-france',      color: 'css-color-wc-france'      },
  germany:     { frame: 'css-frame-wc-germany',     color: 'css-color-wc-germany'     },
  spain:       { frame: 'css-frame-wc-spain',       color: 'css-color-wc-spain'       },
  italy:       { frame: 'css-frame-wc-italy',       color: 'css-color-wc-italy'       },
  portugal:    { frame: 'css-frame-wc-portugal',    color: 'css-color-wc-portugal'    },
  england:     { frame: 'css-frame-wc-england',     color: 'css-color-wc-england'     },
  japan:       { frame: 'css-frame-wc-japan',       color: 'css-color-wc-japan'       },
  korea:       { frame: 'css-frame-wc-korea',       color: 'css-color-wc-korea'       },
  netherlands: { frame: 'css-frame-wc-netherlands', color: 'css-color-wc-netherlands' },
  belgium:     { frame: 'css-frame-wc-belgium',     color: 'css-color-wc-belgium'     },
};

/**
 * Detect Easter Egg Combo.
 *
 * @param frameClass - effectKey của Avatar Frame đang trang bị (vd: 'css-frame-wc-vietnam')
 * @param colorClass - effectKey của Name Color đang trang bị (vd: 'css-color-wc-vietnam')
 * @returns Tên quốc gia nếu combo hợp lệ (vd: 'vietnam'), hoặc null nếu không phải combo
 */
export function detectNationalCombo(
  frameClass?: string | null,
  colorClass?: string | null,
): string | null {
  if (!frameClass || !colorClass) return null;

  const frame = frameClass.trim();
  const color = colorClass.trim();

  for (const [nation, combo] of Object.entries(NATIONAL_COMBOS)) {
    if (combo.frame === frame && combo.color === color) {
      return nation;
    }
  }

  return null;
}
