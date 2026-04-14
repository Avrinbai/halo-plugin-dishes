<script setup lang="ts">
import { computed, toRefs } from 'vue'
import { VButton, VModal, VSwitch } from '@halo-dev/components'

type Period = { id: number; code: 'breakfast' | 'lunch' | 'dinner'; name: string }
type DishForm = {
  categoryId: number
  name: string
  imageUrl: string
  recommendationLevel: number
  description: string
  isAvailable: boolean
  sortOrder: number
  mealPeriodIds: number[]
}
type DishErrors = {
  name?: string
  categoryId?: string
  recommendationLevel?: string
  mealPeriodIds?: string
  imageUrl?: string
}

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  saving: boolean
  form: DishForm
  errors: DishErrors
  periods: Period[]
  categoryOptions: Array<{ value: number; label: string }>
}>()
const { visible, mode, saving, form, errors, periods, categoryOptions } = toRefs(props)

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'save'): void
  (event: 'delete'): void
  (event: 'toggle-period', id: number): void
  (event: 'open-attachment'): void
}>()

const title = computed(() => (props.mode === 'create' ? '新建菜品' : '编辑菜品'))
</script>

<template>
  <VModal :visible="visible" :title="title" @close="emit('close')">
    <div class="plugin-modal-form">
      <div class="plugin-field">
        <label class="plugin-label" for="dish-name">名称</label>
        <input id="dish-name" v-model="form.name" type="text" class="plugin-control" autocomplete="off" />
        <p v-if="errors.name" class="plugin-error">{{ errors.name }}</p>
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="dish-category">分类</label>
        <div class="plugin-select-wrap">
          <select id="dish-category" v-model.number="form.categoryId" class="plugin-control plugin-select">
            <option v-for="o in categoryOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <p v-if="errors.categoryId" class="plugin-error">{{ errors.categoryId }}</p>
      </div>

      <div class="plugin-grid-2">
        <div class="plugin-field">
          <label class="plugin-label" for="dish-rec">推荐等级（1-5）</label>
          <input
            id="dish-rec"
            v-model.number="form.recommendationLevel"
            type="number"
            min="1"
            max="5"
            class="plugin-control"
          />
          <p v-if="errors.recommendationLevel" class="plugin-error">{{ errors.recommendationLevel }}</p>
        </div>
        <div class="plugin-field">
          <label class="plugin-label" for="dish-sort">排序</label>
          <input id="dish-sort" v-model.number="form.sortOrder" type="number" class="plugin-control" />
        </div>
      </div>

      <div class="plugin-field">
        <div class="plugin-label plugin-label--plain">餐段</div>
        <div class="plugin-chips">
          <label v-for="p in periods" :key="p.id" class="plugin-chip">
            <input type="checkbox" :checked="form.mealPeriodIds.includes(p.id)" @change="emit('toggle-period', p.id)" />
            <span>{{ p.name }}</span>
          </label>
        </div>
        <p v-if="errors.mealPeriodIds" class="plugin-error">{{ errors.mealPeriodIds }}</p>
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="dish-image">图片 URL（可选）</label>
        <div class="plugin-inline-row">
          <input id="dish-image" v-model="form.imageUrl" type="text" class="plugin-control plugin-control--grow" />
          <VButton size="sm" type="secondary" class="plugin-attach-btn" @click="emit('open-attachment')">选择附件</VButton>
        </div>
        <p v-if="errors.imageUrl" class="plugin-error">{{ errors.imageUrl }}</p>
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="dish-desc">描述（可选）</label>
        <textarea id="dish-desc" v-model="form.description" rows="3" class="plugin-control plugin-textarea" />
      </div>

      <div class="plugin-switch-card">
        <div class="plugin-switch-text">
          <div class="plugin-switch-title">上架</div>
          <div class="plugin-switch-desc">关闭后前台不会展示/推荐</div>
        </div>
        <VSwitch v-model="form.isAvailable" />
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

.plugin-label--plain {
  margin-bottom: 0.5rem;
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

.plugin-control--grow {
  flex: 1;
  width: auto;
  min-width: 0;
}

.plugin-textarea {
  height: auto;
  min-height: 4.5rem;
  padding: 0.5rem 0.75rem;
  resize: vertical;
  line-height: 1.5;
}

.plugin-select-wrap {
  position: relative;
  width: 100%;
  min-width: 0;
}

.plugin-select {
  display: block;
  width: 100%;
  min-width: 0;
  padding-right: 2rem;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
  background-size: 1rem;
  cursor: pointer;
}

.plugin-inline-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.plugin-attach-btn {
  flex-shrink: 0;
}

.plugin-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  min-width: 0;
}

@media (max-width: 520px) {
  .plugin-grid-2 {
    grid-template-columns: 1fr;
  }
}

.plugin-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.plugin-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  margin: 0;
  padding: 0.375rem 0.625rem;
  font-size: 0.8125rem;
  color: #374151;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 0.375rem;
  cursor: pointer;
  user-select: none;
}

.plugin-chip:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.plugin-chip input {
  margin: 0;
  accent-color: #2563eb;
}

.plugin-switch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0.875rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
}

.plugin-switch-text {
  min-width: 0;
}

.plugin-switch-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.plugin-switch-desc {
  margin-top: 0.125rem;
  font-size: 0.75rem;
  line-height: 1.35;
  color: #6b7280;
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
