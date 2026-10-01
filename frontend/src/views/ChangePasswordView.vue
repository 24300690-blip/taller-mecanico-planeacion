<script setup lang="ts">
import {useRouter} from 'vue-router'
import PasswordForm from '../components/PasswordForm.vue'
import {logout} from '../services/auth'
const router=useRouter()
/** Sale conservando la obligación de cambio en el siguiente ingreso. */
async function exit(){await logout();await router.replace('/')}
</script>
<template><main class="first-access"><section class="card password-card"><span class="eyebrow">Primer acceso</span><h1>Crea tu contraseña</h1><p class="muted">Elige una contraseña personal para continuar.</p><PasswordForm first @done="router.replace('/inicio')"/><button class="text-button" @click="exit">Salir</button></section></main></template>
