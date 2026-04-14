<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const mainBottomPad = computed(() =>
  route.name === 'order'
    ? 'pb-[max(0.5rem,env(safe-area-inset-bottom,0px))]'
    : '',
)
</script>

<template>
  <div
    class="mx-auto flex h-dvh max-h-dvh min-h-0 w-full max-w-md flex-col overflow-hidden bg-slate-50 text-slate-900 antialiased"
  >
    <main
      class="flex min-h-0 flex-1 flex-col overflow-hidden px-4"
      :class="mainBottomPad"
    >
      <slot />
    </main>
  </div>
</template>
