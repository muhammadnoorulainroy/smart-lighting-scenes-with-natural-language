/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution')

module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true
  },
  extends: [
    'plugin:vue/vue3-recommended',
    'eslint:recommended'
  ],
  plugins: ['vue'],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  rules: {
    // Vue-specific rules
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    'vue/component-definition-name-casing': ['error', 'PascalCase'],
    'vue/custom-event-name-casing': ['error', 'camelCase'],
    'vue/define-macros-order': ['error', {
      order: ['defineProps', 'defineEmits']
    }],
    'vue/html-closing-bracket-newline': ['error', {
      singleline: 'never',
      multiline: 'always'
    }],
    'vue/html-indent': ['error', 2],
    'vue/max-attributes-per-line': ['error', {
      singleline: { max: 3 },
      multiline: { max: 1 }
    }],
    'vue/multi-word-component-names': 'off',
    'vue/no-unused-vars': 'error',
    'vue/no-v-html': 'warn',
    'vue/padding-line-between-blocks': ['error', 'always'],
    'vue/prefer-import-from-vue': 'error',
    'vue/prefer-separate-static-class': 'error',
    'vue/prefer-true-attribute-shorthand': 'error',
    'vue/require-default-prop': 'error',
    'vue/require-explicit-emits': 'error',
    'vue/v-on-event-hyphenation': ['error', 'always'],

    // JavaScript rules
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'no-var': 'error',
    'prefer-const': 'error',
    'prefer-template': 'error',
    'eqeqeq': ['error', 'always'],
    'curly': ['error', 'all'],
    'no-duplicate-imports': 'error',
    'no-useless-rename': 'error',
    'object-shorthand': 'error',
    'prefer-arrow-callback': 'error',
    'prefer-destructuring': ['error', {
      array: false,
      object: true
    }],

    // Code quality
    'max-depth': ['warn', 4],
    'max-nested-callbacks': ['warn', 3],
    'max-params': ['warn', 5],
    'complexity': ['warn', 15],
    'no-else-return': 'error',
    'no-lonely-if': 'error',
    'no-unneeded-ternary': 'error'
  },
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        'max-lines': ['warn', { max: 400, skipBlankLines: true, skipComments: true }]
      }
    }
  ],
  ignorePatterns: [
    'dist',
    'node_modules',
    '*.min.js',
    'coverage'
  ]
}

