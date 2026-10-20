const angular = require('@angular-eslint/eslint-plugin');
const angularTemplate = require('@angular-eslint/eslint-plugin-template');
const templateParser = require('@angular-eslint/template-parser');
const tseslint = require('@typescript-eslint/eslint-plugin');
const tsParser = require('@typescript-eslint/parser');

module.exports = [
  { ignores: ['node_modules/**', 'dist/**', 'build/**', 'coverage/**', '.angular/**', '*.min.js'] },
  {
    files: ['**/*.ts'],
    languageOptions: { parser: tsParser, parserOptions: { project: ['./tsconfig.app.json', './tsconfig.spec.json'], tsconfigRootDir: __dirname } },
    plugins: { '@typescript-eslint': tseslint, '@angular-eslint': angular },
    rules: {
      ...tseslint.configs.recommended.rules,
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'sec', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'sec', style: 'kebab-case' }]
    }
  },
  {
    files: ['**/*.html'],
    languageOptions: { parser: templateParser },
    plugins: { '@angular-eslint/template': angularTemplate },
    rules: {
      '@angular-eslint/template/alt-text': 'error',
      '@angular-eslint/template/elements-content': 'error',
      '@angular-eslint/template/label-has-associated-control': 'error',
      '@angular-eslint/template/table-scope': 'error',
      '@angular-eslint/template/valid-aria': 'error'
    }
  }
];
