<script setup lang="ts">
import { computed, toRefs } from 'vue'
import { VButton, VModal } from '@halo-dev/components'

type CategoryForm = {
  name: string
  slug: string
  sortOrder: number
}

type CategoryErrors = {
  name?: string
  slug?: string
}

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  saving: boolean
  form: CategoryForm
  errors: CategoryErrors
}>()
const { visible, mode, saving, form, errors } = toRefs(props)

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'save'): void
  (event: 'delete'): void
}>()

const title = computed(() => (props.mode === 'create' ? '新建分类' : '编辑分类'))
</script>

<template>
  <VModal :visible="visible" :title="title" @close="emit('close')">
    <div class="plugin-modal-form">
      <div class="plugin-field">
        <label class="plugin-label" for="cat-name">名称</label>
        <input id="cat-name" v-model="form.name" type="text" class="plugin-control" autocomplete="off" />
        <p v-if="errors.name" class="plugin-error">{{ errors.name }}</p>
      </div>
      <div class="plugin-field">
        <label class="plugin-label" for="cat-slug">Slug</label>
        <input id="cat-slug" v-model="form.slug" type="text" class="plugin-control" autocomplete="off" />
        <p v-if="errors.slug" class="plugin-error">{{ errors.slug }}</p>
      </div>
      <div class="plugin-field">
        <label class="plugin-label" for="cat-sort">排序</label>
        <input id="cat-sort" v-model.number="form.sortOrder" type="number" class="plugin-control" />
      </div>
    </div>

    <template #footer>
      <div class="plugin-modal-footer">
        <div class="plugin-footer-start">
          <VButton v-if="mode === 'edit'" type="danger" :disabled="saving" @click="emit('delete')">删除</VButton>
        </div>
        <div class="plugin-footer-actions">
          <VButton :disabled="saving" @click="emit('close')">取消</VButton>
          <VButton type="primary" :loading="saving" :disabled="saving" @click="emit('save')">保存</VButton>
        </div>
      </div>
    </template>
  </VModal>
</template>

<style scoped>
.plugin-modal-form {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  min-width: 0;
}

.plugin-field {
  min-width: 0;
}

.plugin-label {
  display: block;
  margin-bottom: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.plugin-control {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 2.25rem;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  line-height: 1.25;
  color: #111827;
  background-color: #fff;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  outline: none;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.plugin-control:hover {
  border-color: #9ca3af;
}

.plugin-control:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgb(59 130 246 / 0.15);
}

.plugin-control::placeholder {
  color: #9ca3af;
}

.plugin-error {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  line-height: 1.35;
  color: #dc2626;
}

.plugin-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  width: 100%;
  flex-wrap: wrap;
}

.plugin-footer-start {
  min-width: 0;
}

.plugin-footer-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
</style>
