<script setup lang="ts">
import {useRoute} from 'vue-router'
/** Errores de navegación dentro del espacio de trabajo. */
const route=useRoute()
</script>
<template><section class="card empty-state"><span class="eyebrow">{{route.path==='/403'?'403':'404'}}</span><h1>{{route.path==='/403'?'No tienes acceso a esta pantalla':'No encontramos esa página'}}</h1><RouterLink class="primary" to="/inicio">Ir a Inicio</RouterLink></section></template>
