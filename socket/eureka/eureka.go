package eureka

import (
	"strconv"
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
		IPAddr:         host,
		VipAddress:     appName,
		Port:           port,
		Status:         fargo.UP,
		StatusPageUrl:  "http://" + host + ":" + strconv.Itoa(port) + "/health",
		HealthCheckUrl: "http://" + host + ":" + strconv.Itoa(port) + "/health",
		DataCenterInfo: fargo.DataCenterInfo{
			Class: "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
			Name:  "MyOwn",
		},
	}

	instance.SetMetadataString("ws-enabled", "true")
	instance.SetMetadataString("ws-path", "/socket/connect")

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
