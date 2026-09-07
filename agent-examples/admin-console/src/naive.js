import { createDiscreteApi } from 'naive-ui';

export const themeOverrides = {
  common: {
    primaryColor: '#4f46e5',
    primaryColorHover: '#4338ca',
    primaryColorPressed: '#3730a3',
    primaryColorSuppl: '#818cf8',
    fontFamily: '"Plus Jakarta Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif',
    fontFamilyMono: '"JetBrains Mono", ui-monospace, SFMono-Regular, Menlo, monospace',
    textColorBase: '#0f172a',
    textColor2: '#334155',
    textColor3: '#64748b',
    borderRadius: '10px',
    borderRadiusSmall: '8px'
  },
  Card: {
    borderRadius: '14px',
    titleFontSizeSmall: '14px',
    titleFontWeight: '700'
  },
  Button: {
    borderRadiusMedium: '10px',
    fontWeight: '600'
  },
  Tag: {
    borderRadius: '6px',
    fontSizeTiny: '11px',
    fontSizeSmall: '12px'
  }
};

export const { message, dialog, loadingBar } = createDiscreteApi(
  ['message', 'dialog', 'loadingBar'],
  { configProviderProps: { themeOverrides } }
);
