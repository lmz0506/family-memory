<script setup lang="ts">
withDefaults(defineProps<{variant?:'memory'|'capsule'|'family'}>(),{variant:'memory'})
const stars=[[62,190],[136,115],[223,151],[292,76],[367,129],[430,64],[477,196],[382,253],[278,223],[185,285],[87,267]]
</script>

<template>
  <div class="star-chart" :class="`star-chart-${variant}`" aria-hidden="true">
    <svg viewBox="0 0 540 360" fill="none">
      <ellipse class="chart-orbit" cx="270" cy="180" rx="218" ry="112" transform="rotate(-24 270 180)"/>
      <ellipse class="chart-orbit chart-orbit-outer" cx="270" cy="180" rx="255" ry="145" transform="rotate(-24 270 180)"/>
      <path class="chart-thread" :d="variant==='family'?'M62 190L136 115L223 151L292 76L367 129L430 64 M223 151L278 223L382 253L477 196 M278 223L185 285L87 267L62 190':variant==='capsule'?'M62 190Q223 10 477 196 M136 115L223 151L278 223L382 253':'M62 190L136 115L223 151L292 76L367 129L477 196L382 253L278 223L185 285'"/>
      <g v-for="([x,y],i) in stars" :key="i">
        <circle class="chart-halo" :cx="x" :cy="y" :r="i===3?18:9"/>
        <circle class="chart-star" :cx="x" :cy="y" :r="i===3?4:2.4"/>
        <path v-if="i===3||i===7" class="chart-ray" :d="`M${x-11} ${y}h22 M${x} ${y-11}v22`"/>
      </g>
      <circle v-if="variant==='capsule'" class="chart-planet" cx="278" cy="223" r="26"/>
      <text x="313" y="57">{{variant==='memory'?'那些闪光的日子':variant==='capsule'?'寄往未来的星光':'我们，彼此相连'}}</text>
      <text class="chart-coordinate" x="33" y="330">{{variant==='memory'?'MEMORY CONSTELLATION':variant==='capsule'?'LIGHT TO THE FUTURE':'OUR LITTLE UNIVERSE'}}</text>
    </svg>
  </div>
</template>

<style scoped>
.star-chart{pointer-events:none;position:relative;width:100%;color:#e9c698}.star-chart svg{width:100%;height:100%;overflow:visible}.chart-orbit{stroke:currentColor;stroke-opacity:.19;stroke-width:.7}.chart-orbit-outer{stroke-dasharray:2 9;stroke-opacity:.22}.chart-thread{stroke:currentColor;stroke-opacity:.42;stroke-width:1}.chart-star{fill:#ffe6bd;filter:drop-shadow(0 0 5px #f2c68e)}.chart-halo{fill:#f4c387;opacity:.07}.chart-ray{stroke:#ffe1ae;stroke-width:.7}.chart-planet{stroke:#eac49b;stroke-opacity:.5;stroke-dasharray:2 4}.star-chart text{fill:#c4b7a7;font-size:10px;letter-spacing:2px}.star-chart .chart-coordinate{font-size:8px;letter-spacing:3px;fill:#8c92ab}.star-chart-family{color:#a6b9db}.star-chart-capsule{color:#c2acd9}
</style>
