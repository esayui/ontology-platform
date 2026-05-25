<template>
  <div class="monaco-editor-wrapper">
    <div ref="editorContainer" class="monaco-container" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps<{
  modelValue: string
  readOnly?: boolean
  language?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editorContainer = ref<HTMLDivElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  if (!editorContainer.value) return

  // Register Drools DRL language (Java-based syntax highlighting)
  monaco.languages.register({ id: 'drl' })
  monaco.languages.setMonarchTokensProvider('drl', {
    tokenizer: {
      root: [
        [/\b(package|import|rule|when|then|end|global|function|dialect|salience|no-loop|lock-on-active|agenda-group|activation-group|date-effective|date-expires|enabled|duration)\b/, 'keyword'],
        [/\b(boolean|byte|char|double|float|int|long|short|String|void)\b/, 'type'],
        [/\b(true|false|null)\b/, 'constant'],
        [/\b(if|else|for|while|return|new|this|throw|try|catch|finally)\b/, 'keyword'],
        [/\/\/.*$/, 'comment'],
        [/\/\*/, 'comment', '@comment'],
        [/"/, 'string', '@string'],
        [/[{}()[\]]/, 'delimiter'],
        [/[a-zA-Z_]\w*/, 'identifier'],
      ],
      comment: [
        [/\*\//, 'comment', '@pop'],
        [/./, 'comment'],
      ],
      string: [
        [/"/, 'string', '@pop'],
        [/./, 'string'],
      ],
    },
  })

  editor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: props.language || 'drl',
    theme: 'vs-dark',
    fontSize: 14,
    lineNumbers: 'on',
    minimap: { enabled: true },
    automaticLayout: true,
    readOnly: props.readOnly || false,
    wordWrap: 'on',
    tabSize: 4,
    scrollBeyondLastLine: false,
  })

  editor.onDidChangeModelContent(() => {
    const value = editor?.getValue() || ''
    emit('update:modelValue', value)
  })
})

onUnmounted(() => {
  editor?.dispose()
})

watch(() => props.modelValue, (newVal) => {
  if (editor && editor.getValue() !== newVal) {
    editor.setValue(newVal)
  }
})

defineExpose({ getEditor: () => editor })
</script>

<style scoped>
.monaco-editor-wrapper {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
.monaco-container {
  width: 100%;
  height: 100%;
}
</style>
