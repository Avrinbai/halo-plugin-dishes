<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Dialog, Toast, VButton, VCard, VPageHeader } from '@halo-dev/components'
import { utils } from '@halo-dev/ui-shared'
import { getApiErrorMessage, getData, putData } from '@/api/client'
import RiSettings3Line from '~icons/ri/settings-3-line'

type AccessMode = 'none' | 'password'

type SettingsResp = {
  basic: {
    accessMode: AccessMode
    accessPassword: string
    accessPasswordSet: boolean
    publicAccessUrl: string
    publicLogoUrl: string
    defaultPublicAccessUrl: string
  }
  notify: {
    enabled: boolean
    channel: string
    webhookUrl: string
    orderNowEnabled: boolean
    orderReservationEnabled: boolean
  }
}

const loading = ref(true)
const saving = ref(false)
const activeTab = ref<'basic' | 'notify'>('basic')
const logoAttachmentModalOpen = ref(false)

const form = reactive<SettingsResp>({
  basic: {
    accessMode: 'none',
    accessPassword: '',
    accessPasswordSet: false,
    publicAccessUrl: '',
    publicLogoUrl: '',
    defaultPublicAccessUrl: '/dishes',
  },
  notify: {
    enabled: false,
    channel: '',
    webhookUrl: '',
    orderNowEnabled: true,
    orderReservationEnabled: false,
  },
})

const accessModeOptions: Array<{ value: AccessMode; label: string; desc: string }> = [
  { value: 'none', label: '无需鉴权', desc: '前台可直接访问并点菜，无需登录或密码。' },
  { value: 'password', label: '密码访问', desc: '首次访问需输入密码，本地缓存有效期内免重复输入。' },
]

const needPasswordInput = computed(() => form.basic.accessMode === 'password')
const logoPreviewUrl = computed(() => {
  const raw = (form.basic.publicLogoUrl || '').trim()
  if (!raw) return ''
  return utils.attachment.getThumbnailUrl(raw, 'M')
})

type AttachmentLike = {
  status?: Record<string, unknown>
  spec?: Record<string, unknown>
  permalink?: string
  url?: string
  path?: string
}

function asString(v: unknown) {
  return typeof v === 'string' ? v : ''
}

function onLogoAttachmentSelect(attachments: AttachmentLike[]) {
  const first = attachments?.[0]
  const status = first?.status ?? {}
  const spec = first?.spec ?? {}
  const url =
    asString(status.permalink) ||
    asString(status.url) ||
    asString(status.link) ||
    asString(status.publicUrl) ||
    asString(status.downloadUrl) ||
    asString(spec.permalink) ||
    asString(spec.url) ||
    asString(spec.link) ||
    asString(spec.path) ||
    asString(first?.permalink) ||
    asString(first?.url) ||
    asString(first?.path) ||
    ''

  if (url.trim()) {
    form.basic.publicLogoUrl = url.trim()
    return
  }
  Dialog.warning({ title: '选择失败', description: '未从附件中解析到可用图片地址，请换一个附件重试。' })
}

async function load() {
  loading.value = true
  try {
    const data = await getData<SettingsResp>('/settings')
    form.basic.accessMode = data.basic?.accessMode ?? 'none'
    form.basic.accessPassword = data.basic?.accessPassword ?? ''
    form.basic.accessPasswordSet = !!data.basic?.accessPasswordSet
    form.basic.defaultPublicAccessUrl = data.basic?.defaultPublicAccessUrl ?? '/dishes'
    form.basic.publicAccessUrl = data.basic?.publicAccessUrl || form.basic.defaultPublicAccessUrl
    form.basic.publicLogoUrl = data.basic?.publicLogoUrl ?? ''
    form.notify.enabled = !!data.notify?.enabled
    form.notify.channel = data.notify?.channel ?? ''
    form.notify.webhookUrl = data.notify?.webhookUrl ?? ''
    form.notify.orderNowEnabled = data.notify?.orderNowEnabled ?? true
    form.notify.orderReservationEnabled = data.notify?.orderReservationEnabled ?? false
  } catch (e) {
    Dialog.error({
      title: '加载失败',
      description: getApiErrorMessage(e, '加载设置失败，请稍后重试', {
        DISHES_ACCESS_DENIED: '当前账号无权访问该设置',
      }),
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  if (needPasswordInput.value && !form.basic.accessPassword.trim()) {
    Dialog.warning({ title: '提示', description: '密码访问模式下请先填写前台访问密码。' })
    return
  }
  saving.value = true
  try {
    await putData('/settings', {
      basic: {
        accessMode: form.basic.accessMode,
        accessPassword: form.basic.accessPassword,
        publicAccessUrl: form.basic.publicAccessUrl,
        publicLogoUrl: form.basic.publicLogoUrl,
      },
      notifyConfig: {
        enabled: form.notify.enabled,
        channel: form.notify.channel,
        webhookUrl: form.notify.webhookUrl,
        orderNowEnabled: form.notify.orderNowEnabled,
        orderReservationEnabled: form.notify.orderReservationEnabled,
      },
    })
    Toast.success('设置已保存')
    await load()
  } catch (e) {
    Dialog.error({
      title: '保存失败',
      description: getApiErrorMessage(e, '保存失败，请稍后重试', {
        DISHES_BAD_REQUEST: '参数校验失败，请检查配置后重试',
      }),
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <VPageHeader title="插件设置">
    <template #icon>
      <RiSettings3Line />
    </template>
  </VPageHeader>

  <div class="settings-page :uno: p-4">
    <AttachmentSelectorModal
      v-model:visible="logoAttachmentModalOpen"
      :max="1"
      :accepts="['image/*']"
      @select="onLogoAttachmentSelect"
    />

    <VCard class="settings-card" :body-class="[':uno: !p-0']">
      <template #header>
        <div class="settings-tabs">
          <button
            type="button"
            class="settings-tab"
            :class="{ 'settings-tab--active': activeTab === 'basic' }"
            @click="activeTab = 'basic'"
          >
            基础设置
          </button>
          <button
            type="button"
            class="settings-tab"
            :class="{ 'settings-tab--active': activeTab === 'notify' }"
            @click="activeTab = 'notify'"
          >
            通知设置
          </button>
        </div>
      </template>

      <div class="settings-body">
        <div v-if="loading" class="settings-loading">加载中…</div>

        <template v-else>
          <div v-show="activeTab === 'basic'" class="settings-section">
            <h3 class="settings-title">前台访问策略设置</h3>

            <div class="settings-mode-list">
              <label v-for="opt in accessModeOptions" :key="opt.value" class="settings-mode-item">
                <input v-model="form.basic.accessMode" type="radio" name="access-mode" :value="opt.value" />
                <div class="settings-mode-content">
                  <div class="settings-mode-label">{{ opt.label }}</div>
                  <div class="settings-mode-desc">{{ opt.desc }}</div>
                </div>
              </label>
            </div>

            <div v-if="needPasswordInput" class="settings-field">
              <div class="settings-field-label">前台访问密码</div>
              <input
                v-model="form.basic.accessPassword"
                type="password"
                class="settings-input"
                placeholder="请输入前台访问密码"
              />
              <p v-if="form.basic.accessPasswordSet" class="settings-hint">已设置密码，留空将清空。</p>
            </div>

            <div class="settings-field">
              <div class="settings-field-label">自定义前台访问链接</div>
              <input
                v-model="form.basic.publicAccessUrl"
                type="text"
                class="settings-input"
                placeholder="例如：/dishes"
              />
              <p class="settings-hint">
                当前默认路径：{{ form.basic.defaultPublicAccessUrl }}；留空则使用默认前台入口链接。
              </p>
            </div>

            <div class="settings-field">
              <div class="settings-field-label">前台 Logo</div>
              <div class="settings-logo-row">
                <div class="settings-logo-preview-wrap">
                  <img v-if="logoPreviewUrl" :src="logoPreviewUrl" alt="logo" class="settings-logo-preview" />
                  <span v-else class="settings-logo-placeholder">未设置</span>
                </div>
                <div class="settings-logo-actions">
                  <VButton size="sm" @click="logoAttachmentModalOpen = true">从附件库选择</VButton>
                  <VButton size="sm" type="secondary" :disabled="!form.basic.publicLogoUrl" @click="form.basic.publicLogoUrl = ''">
                    清空
                  </VButton>
                </div>
              </div>
              <p class="settings-hint">未设置时前台将继续使用默认 PNG 图标。</p>
            </div>
          </div>

          <div v-show="activeTab === 'notify'" class="settings-section">
            <h3 class="settings-title">通知设置</h3>
            <p class="settings-desc">点菜提交后可推送企业微信群机器人消息卡片。</p>
            <label class="settings-checkbox">
              <input v-model="form.notify.enabled" type="checkbox" />
              <span>启用通知</span>
            </label>
            <div class="settings-field">
              <div class="settings-field-label">企业微信群机器人 Webhook</div>
              <input
                v-model="form.notify.webhookUrl"
                type="text"
                class="settings-input"
                placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
              />
            </div>
            <label class="settings-checkbox">
              <input v-model="form.notify.orderNowEnabled" type="checkbox" :disabled="!form.notify.enabled" />
              <span>立即点菜通知</span>
            </label>
            <label class="settings-checkbox">
              <input v-model="form.notify.orderReservationEnabled" type="checkbox" :disabled="!form.notify.enabled" />
              <span>预约点菜通知</span>
            </label>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="settings-footer">
          <VButton :disabled="loading || saving" @click="load">重置</VButton>
          <VButton type="primary" :disabled="loading || saving" @click="save">保存</VButton>
        </div>
      </template>
    </VCard>
  </div>
</template>

<style scoped>
.settings-card {
  box-shadow: 0 4px 18px rgb(15 23 42 / 0.06);
}

.settings-card :deep(.card-header),
.settings-card :deep(.card-footer) {
  padding: 0;
}

.settings-card :deep(.card-header) {
  background: #fafafa;
}

.settings-tabs {
  display: flex;
  border-bottom: 1px solid rgb(228 228 231);
  background: #fafafa;
}

.settings-tab {
  border: none;
  background: transparent;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: rgb(82 82 91);
  cursor: pointer;
}

.settings-tab--active {
  color: rgb(24 24 27);
  font-weight: 600;
  box-shadow: inset 0 -2px 0 rgb(24 24 27);
}

.settings-body {
  padding: 1rem;
}

.settings-loading {
  color: rgb(113 113 122);
  font-size: 0.875rem;
  padding: 0.5rem 0;
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.settings-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: rgb(24 24 27);
}

.settings-desc {
  margin: 0;
  font-size: 0.875rem;
  color: rgb(113 113 122);
}

.settings-mode-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.settings-mode-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  border: 1px solid rgb(228 228 231);
  border-radius: 8px;
  padding: 0.625rem 0.75rem;
}

.settings-mode-content {
  min-width: 0;
}

.settings-mode-label {
  font-size: 0.875rem;
  font-weight: 600;
}

.settings-mode-desc {
  font-size: 0.8125rem;
  color: rgb(113 113 122);
}

.settings-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.settings-logo-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.settings-logo-preview-wrap {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid rgb(212 212 216);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fff;
}

.settings-logo-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.settings-logo-placeholder {
  font-size: 10px;
  color: rgb(113 113 122);
}

.settings-logo-actions {
  display: inline-flex;
  gap: 0.5rem;
}

.settings-field-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(63 63 70);
}

.settings-input {
  height: 2.25rem;
  border: 1px solid rgb(212 212 216);
  border-radius: 6px;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}

.settings-hint {
  margin: 0;
  font-size: 0.75rem;
  color: rgb(113 113 122);
}

.settings-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.settings-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
}
</style>
