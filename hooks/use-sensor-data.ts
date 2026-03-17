'use client'

import useSWR from 'swr'
import { SensorData } from '@/lib/types'
import { getApiUrl } from '@/lib/api-config'

const fetcher = (url: string) => fetch(url).then(res => res.json())

export function useSensorData() {
  const { data, error, isLoading, mutate } = useSWR<SensorData>(
    getApiUrl('/api/sensores/latest'),
    fetcher,
    {
      refreshInterval: 2500, // Update every 2.5 seconds
      revalidateOnFocus: true,
      dedupingInterval: 1000
    }
  )

  return {
    data,
    isLoading,
    isError: !!error,
    error,
    refresh: mutate
  }
}
