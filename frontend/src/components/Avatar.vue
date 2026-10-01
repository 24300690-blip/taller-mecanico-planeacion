<script setup lang="ts">
import {computed} from 'vue'
import {authState} from '../services/auth'
/** Iniciales de respaldo para el avatar compartido. */
const initials=computed(()=>authState.user?.fullName.split(' ').filter(Boolean).slice(0,2).map(x=>x[0]).join('')||'TU')
</script>
<template><span class="avatar"><img v-if="authState.photo" :src="authState.photo" alt="Foto de perfil"/><span v-else>{{initials}}</span></span></template>
