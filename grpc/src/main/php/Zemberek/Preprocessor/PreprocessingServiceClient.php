<?php
// GENERATED CODE -- DO NOT EDIT!

namespace Zemberek\Preprocessor;

/**
 */
class PreprocessingServiceClient extends \Grpc\BaseStub {

    /**
     * @param string $hostname hostname
     * @param array $opts channel options
     * @param \Grpc\Channel $channel (optional) re-use channel object
     */
    public function __construct($hostname, $opts, $channel = null) {
        parent::__construct($hostname, $opts, $channel);
    }

    /**
     * @param \Zemberek\Preprocessor\TokenizationRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function Tokenize(\Zemberek\Preprocessor\TokenizationRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.preprocessor.PreprocessingService/Tokenize',
        $argument,
        ['\Zemberek\Preprocessor\TokenizationResponse', 'decode'],
        $metadata, $options);
    }

    /**
     * @param \Zemberek\Preprocessor\SentenceExtractionRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function ExtractSentences(\Zemberek\Preprocessor\SentenceExtractionRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.preprocessor.PreprocessingService/ExtractSentences',
        $argument,
        ['\Zemberek\Preprocessor\SentenceExtractionResponse', 'decode'],
        $metadata, $options);
    }

}
