package eureka

import (
	"time"

	"github.com/hudl/fargo"
	"github.com/nteshxx/chatwolf2/socket/internal/utils"
)

func RegisterWithEureka(eurekaURL, appName, host string, port int) (func(), error) {
	// Create a new Eureka connection
	conn := fargo.NewConn(eurekaURL)

	// Define your application instance info
	instance := fargo.Instance{
		App:            appName,
		HostName:       host,
		IPAddr:         "127.0.0.1",
		Port:           port,
		Status:         fargo.UP,
		StatusPageUrl:  "http://" + host + ":7200/socket/health",
		HealthCheckUrl: "http://" + host + ":7200/socket/health",
		DataCenterInfo: fargo.DataCenterInfo{
			Class: "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
			Name:  "MyOwn",
		},
	}

	// Register the instance with Eureka
	if err := conn.RegisterInstance(&instance); err != nil {
		utils.Log.Fatal().Msg("failed to register with Eureka")
	}

	// run heartbeats in background so this function returns
	go func() {
		ticker := time.NewTicker(30 * time.Second)
		defer ticker.Stop()
		for range ticker.C {
			if err := conn.HeartBeatInstance(&instance); err != nil {
				utils.Log.Error().Err(err).Msg("failed to send heartbeat")
			}
		}
	}()

	// Define deregister function
	deregister := func() {
		if err := conn.DeregisterInstance(&instance); err != nil {
			utils.Log.Error().Err(err).Msg("failed to deregister from Eureka")
		} else {
			utils.Log.Info().Msg("deregistered from Eureka")
		}
	}

	return deregister, nil
}
