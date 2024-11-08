import {ref, watch, onBeforeUnmount, Ref} from 'vue';
type Callback = () => void;

export function useWatchInterval(source : Ref<boolean>, callback: Callback, interval = 60000) {
  const intervalId = ref<ReturnType<typeof setInterval> | null>(null);

  const startInterval = () => {
    intervalId.value = setInterval(callback, interval);
  };

  const stopInterval = () => {
    if (intervalId.value !== null) {
      clearInterval(intervalId.value);
      intervalId.value = null;
    }
  };

  watch(source, (newValue) => {
    if (newValue) {
      startInterval();
      callback(); // Call immediately when the source changes
    } else {
      stopInterval();
    }
  }, {immediate: true});

  onBeforeUnmount(stopInterval);
}
